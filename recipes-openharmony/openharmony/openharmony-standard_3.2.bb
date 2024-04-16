# SPDX-FileCopyrightText: Huawei Inc.
#
# SPDX-License-Identifier: Apache-2.0

SUMMARY = "OpenHarmony 3.2"


LICENSE = "0BSD & Apache-2.0 & BSD-2-Clause & BSD-3-Clause & BSL-1.0 & \
GPL-2.0-only & GPL-2.0-or-later & GPL-2-with-bison-exception & GPL-3.0-only & \
LGPL-2.1-only & LGPL-2.1-or-later & LGPL-3.0-only & CPL-1.0 & MIT & MIT-0 & \
MIT-Modern-Variant & Zlib & CC-BY-3.0 & CC-BY-SA-3.0 & CC-BY-NC-SA-3.0 & X11 & \
PD & OFL-1.1 & OpenSSL & MulanPSL-2.0 & bzip2-1.0.6 & ISC & ICU & IJG & Libpng & \
MPL-1.1 & MPL-2.0 & FTL"
LIC_FILES_CHKSUM = "file://build/LICENSE;md5=cfba563cea4ce607306f8a392f19bf6c"

DEPENDS += "bison-native"
DEPENDS += "perl-native"
DEPENDS += "ruby-native"
DEPENDS += "ncurses-native"
DEPENDS += "ccache-native"
DEPENDS += "clang-native"
DEPENDS += "nodejs-native"

FILESEXTRAPATHS:prepend := "${THISDIR}/openharmony-${PV}:"
FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}-${PV}:"

include ${PN}-sources-${PV}.inc

require java-tools.inc

inherit ccache

OHOS_PRODUCT_NAME="rpi4"
B = "${S}/out/rpi4"

SRC_URI += "file://prebuilts_download.sh"
SRC_URI += "file://prebuilts_download.py"

SRC_URI += "file://hdi-gen-compiler.patch;patchdir=${S}/drivers/hdf_core"
SRC_URI += "file://rpi4-config-json.patch;patchdir=${S}/vendor/iscas"

# clean build directory if already exists
clean_out_dir() {
    if [ -d ${S}/out ]; then
        rm -r ${S}/out
    fi
}

# The task is usually done by //build/prebuilts_download.sh but we are 
# skipping the download part which is done by do_fetch,
# we are extracting here only the main installation part of the prebuilts
prebuilts_download() {
    python3 ${WORKDIR}/prebuilts_download.py --host-cpu x86_64 --host-platform linux --code-dir ${S}
    chmod +x ${WORKDIR}/prebuilts_download.sh
    ${WORKDIR}/prebuilts_download.sh ${S}
}

python_is_python3() { 
    PYTHON3_DIR=$(dirname $(which python3))
    if [ ! -f "${PYTHON3_DIR}/python" ]; then
        ln -sf "${PYTHON3_DIR}/python3" "${PYTHON3_DIR}/python"
    else
        bbnote "not creating symlink, python already exists in ${PYTHON3_DIR}"
    fi
}

do_configure[prefuncs] += "clean_out_dir prebuilts_download python_is_python3"

do_compile:prepend() {
    # Remove unused Makefile to avoid bitbake running oe_runmake
    rm -f ${S}/out/rpi4/Makefile
}

do_compile:append() {
    # Find the path of the g++ compiler
    GPP_PATH=$(which g++)
    if [ -z "$GPP_PATH" ]; then
        bberr "g++ not found."
        exit 1
    fi

    # Get the directory where g++ is located
    GPP_DIR=$(dirname "$GPP_PATH")

    # Check if c++ exists in the same directory
    if [ ! -f "$GPP_DIR/c++" ]; then
        # If c++ does not exist, create a symlink to g++
        ln -s "$GPP_PATH" "$GPP_DIR/c++"
        bbnote "Symbolic link for c++ created."
    fi

    # set the compiler to Yocto native toolchain.
    CC="${BUILD_CC}"
    CXX="${BUILD_CXX}"
    FC="${BUILD_FC}"
    CPP="${BUILD_CPP}"
    LD="${BUILD_LD}"
    CCLD="${BUILD_CCLD}"
    AR="${BUILD_AR}"
    AS="${BUILD_AS}"
    RANLIB="${BUILD_RANLIB}"
    STRIP="${BUILD_STRIP}"
    NM="${BUILD_NM}"

    cd ${S}
    bash ${S}/build.sh --product-name ${OHOS_PRODUCT_NAME} --ccache
}

do_install () {
    OHOS_PACKAGE_OUT_DIR="${B}/packages/phone"
    bbnote "installing contents from ${OHOS_PACKAGE_OUT_DIR} to ${D}"

    # We install library files to /lib and executables into /bin, and
    # then setup /system/lib and /system/bin symlinks to avoid breaking use of
    # hard-coded paths.
    mkdir -p ${D}/system ${D}/lib ${D}/bin
    cp -r ${OHOS_PACKAGE_OUT_DIR}/system/lib/* ${D}/lib/
    [ -d "${OHOS_PACKAGE_OUT_DIR}/system/lib64" ] && cp -r ${OHOS_PACKAGE_OUT_DIR}/system/lib64/* ${D}/lib/
    cp -r ${OHOS_PACKAGE_OUT_DIR}/system/bin/* ${D}/bin/
    find ${D}/bin/ -type f -exec chmod 755 {} \;
    ln -sfT ../lib ${D}/system/lib
    [ -d "${OHOS_PACKAGE_OUT_DIR}/system/lib64" ] && ln -sfT ../lib ${D}/system/lib64
    ln -sfT ../bin ${D}/system/bin

    # OpenHarmony etc (configuration) files
    mkdir -p ${D}${sysconfdir}
    cp -r  ${OHOS_PACKAGE_OUT_DIR}/system/etc/* ${D}${sysconfdir}
    ln -sfT ..${sysconfdir} ${D}/system/etc

    # system ability configurations
    mkdir -p ${D}/system/profile
    cp -r ${OHOS_PACKAGE_OUT_DIR}/system/profile/* ${D}/system/profile

    # copy /system/usr
    mkdir -p ${D}/system/usr
    [ -d "${OHOS_PACKAGE_OUT_DIR}/system/usr" ] && cp -r ${OHOS_PACKAGE_OUT_DIR}/system/usr/* ${D}/system/usr

    # OpenHarmony font files
    mkdir -p ${D}/system/fonts
    [ -d "${OHOS_PACKAGE_OUT_DIR}/system/fonts" ] && cp -r ${OHOS_PACKAGE_OUT_DIR}/system/fonts/* ${D}/system/fonts

    # OpenHarmony app files
    mkdir -p ${D}/system/app
    [ -d "${OHOS_PACKAGE_OUT_DIR}/system/app" ] && cp -r ${OHOS_PACKAGE_OUT_DIR}/system/app/* ${D}/system/app

    # copy /vendor
    [ -d "${OHOS_PACKAGE_OUT_DIR}/vendor" ] && mkdir -p ${D}/vendor
    [ -d "${OHOS_PACKAGE_OUT_DIR}/vendor" ] && cp -r  ${OHOS_PACKAGE_OUT_DIR}/vendor/* ${D}/vendor

    # initialize root file system 
    cd ${D}
    mkdir  chip_prod  config  data  dev  eng_chipset  eng_system  \
        mnt  module_update  proc  storage  sys  sys_prod  tmp  updater 
    ln -sf /vendor ${D}/chipset
    ln -sf /system/bin/init ${D}/init

    # exclude some libs and bins because conflicting with other yocto packages
    rm ${D}/bin/sh
    [ -d "${D}/etc/profile" ] && rm -r ${D}/etc/profile
    [ -d "${D}/etc/udev" ] && rm -r ${D}/etc/udev
    rm -r ${D}/lib/firmware

    # rename musl to avoid conflict with yocto provided libc
    if [ "${TARGET_ARCH}" = "aarch64" ]; then
        mv ${D}/lib/ld-musl-aarch64.so.1 ${D}/lib/ohos-ld-musl-aarch64.so.1
        mv ${D}/etc/ld-musl-aarch64.path ${D}/etc/ohos-ld-musl-aarch64.path
    fi

    if [ "${TARGET_ARCH}" = "arm" ]; then
        mv ${D}/lib/ld-musl-arm.so.1 ${D}/lib/ohos-ld-musl-arm.so.1
        mv ${D}/etc/ld-musl-arm.path ${D}/etc/ohos-ld-musl-arm.path
    fi

    # removing artifact which architecture does not match the target
    rm ${D}/lib/libext2_uuid.so
    rm ${D}/lib/libf2fs.so

    return 0
}

# OpenHarmony libraries are not versioned properly.
# Move the unversioned .so files to the primary package.
SOLIBS = ".so"
FILES_SOLIBSDEV = ""

FILES:${PN} += "\
    /system/* \
    /vendor/* \
    /lib/* \
    chip_prod  chipset  config  data  dev  eng_chipset  eng_system  \
    init  mnt  module_update  proc  storage  sys  sys_prod  tmp  updater \
"


EXCLUDE_FROM_SHLIBS = "1"

# To avoid excessive diskspace blowup, we are stripping our executables
INSANE_SKIP:${PN} += "already-stripped"

# Need this to allow libnative_window.so and libnative_drawing.so symlinks
INSANE_SKIP:${PN} += "dev-so"

# TEMPORARY fix to: `do_package_qa: QA Issue: /lib/init/reboot/librebootmodule.z.so 
# contained in package openharmony-standard requires libinit_module_engine.so()(64bit), 
# but no providers found in RDEPENDS:openharmony-standard? [file-rdeps]`
INSANE_SKIP:${PN} += "file-rdeps"
