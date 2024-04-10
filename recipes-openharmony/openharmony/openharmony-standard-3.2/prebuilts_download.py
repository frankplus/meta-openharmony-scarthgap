#!/usr/bin/python3
# Copyright (c) 2024 Huawei Device Co., Ltd.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import json
import os
import shutil
import subprocess
import argparse

def _run_cmd(cmd):
    res = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                        stderr=subprocess.PIPE)
    sout, serr = res.communicate()
    return sout.rstrip().decode('utf-8'), serr, res.returncode

def _node_modules_copy(config, code_dir, enable_symlink):
    for config_info in config:
        src_dir = os.path.join(code_dir, config_info.get('src'))
        dest_dir = os.path.join(code_dir, config_info.get('dest'))
        use_symlink = config_info.get('use_symlink')
        if os.path.exists(os.path.dirname(dest_dir)):
            shutil.rmtree(os.path.dirname(dest_dir))
        if use_symlink == 'True' and enable_symlink == True:
            os.makedirs(os.path.dirname(dest_dir), exist_ok=True)
            os.symlink(src_dir, dest_dir)
        else:
            shutil.copytree(src_dir, dest_dir, symlinks=True)

def _file_handle(config, code_dir, host_platform):
    for config_info in config:
        src_dir = code_dir + config_info.get('src')
        dest_dir = code_dir + config_info.get('dest')
        tmp_dir = config_info.get('tmp')
        symlink_src = config_info.get('symlink_src')
        symlink_dest = config_info.get('symlink_dest')
        rename = config_info.get('rename')
        if os.path.exists(src_dir):
            if tmp_dir:
                tmp_dir = code_dir + tmp_dir
                shutil.move(src_dir, tmp_dir)
                cmd = 'mv {}/*.mark {}'.format(dest_dir, tmp_dir)
                _run_cmd(cmd)
                if os.path.exists(dest_dir):
                    shutil.rmtree(dest_dir)
                shutil.move(tmp_dir, dest_dir)
            elif rename:
                if os.path.exists(dest_dir) and dest_dir != src_dir:
                    shutil.rmtree(dest_dir)
                shutil.move(src_dir, dest_dir)
                if symlink_src and symlink_dest:
                    if os.path.exists(dest_dir + symlink_dest):
                        os.remove(dest_dir + symlink_dest)
                    if host_platform == 'darwin' and os.path.basename(dest_dir) == "nodejs":
                        symlink_src = symlink_src.replace('linux', 'darwin')
                    os.symlink(os.path.basename(symlink_src), dest_dir + symlink_dest)
            else:
                _run_cmd('chmod 755 {} -R'.format(dest_dir))

def _install(config, code_dir):
    for config_info in config:
        install_dir = '{}/{}'.format(code_dir, config_info.get('install_dir'))
        script = config_info.get('script')
        cmd = '{}/{}'.format(install_dir, script)
        args = config_info.get('args')
        for arg in args:
            for key in arg.keys():
                cmd = '{} --{}={}'.format(cmd, key, arg[key])
        dest_dir = '{}/{}'.format(code_dir, config_info.get('destdir'))
        cmd = '{} --destdir={}'.format(cmd, dest_dir)
        _run_cmd(cmd)

def read_json_file(file_path):
    with open(file_path, 'r') as file:
        return json.load(file)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host-cpu', help='host cpu', required=True)
    parser.add_argument('--host-platform', help='host platform', required=True)
    parser.add_argument('--code-dir', help='openharmony code root directory')
    args = parser.parse_args()

    config_file_path = os.path.join(args.code_dir, 'build/prebuilts_download_config.json')

    # Load the configuration
    config = read_json_file(config_file_path)

    # Extract the file_handle_config part
    file_handle_config = config.get('file_handle_config', [])

    # Execute file handling based on the extracted configuration
    _file_handle(file_handle_config, args.code_dir, args.host_platform)

    # Extract node modules copy configuration and execute
    node_modules_copy_config = config.get('node_modules_copy')
    _node_modules_copy(node_modules_copy_config, args.code_dir, enable_symlink=False)

    # Extract the install configuration
    install_config = config.get(args.host_platform).get(args.host_cpu).get('install')

    if install_config:
        # Execute the install scripts as specified in the JSON file
        _install(install_config, args.code_dir)

if __name__ == '__main__':
    main()
