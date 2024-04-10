# SPDX-FileCopyrightText: Huawei Inc.
#
# SPDX-License-Identifier: Apache-2.0

SUMMARY = "A console-only image with OpenHarmony first party components"

LICENSE = "Apache-2.0"

inherit core-image

# install OpenHarmony components and ptests
IMAGE_INSTALL += "openharmony-standard"

# Let's be friendly enough to provide a fully working interactive shell
IMAGE_INSTALL += "bash"

use_ohos_musl() {
    if [ "${TARGET_ARCH}" = "aarch64" ]; then
        rm ${IMAGE_ROOTFS}/lib/ld-musl-aarch64.so.1
        rm ${IMAGE_ROOTFS}/etc/ld-musl-aarch64.path

        mv ${IMAGE_ROOTFS}/lib/ohos-ld-musl-aarch64.so.1 ${IMAGE_ROOTFS}/lib/ld-musl-aarch64.so.1 
        mv ${IMAGE_ROOTFS}/etc/ohos-ld-musl-aarch64.path ${IMAGE_ROOTFS}/etc/ld-musl-aarch64.path 
    fi

    if [ "${TARGET_ARCH}" = "arm" ]; then
        rm ${IMAGE_ROOTFS}/lib/ld-musl-armhf.so.1
        rm ${IMAGE_ROOTFS}/etc/ld-musl-armhf.path

        mv ${IMAGE_ROOTFS}/lib/ohos-ld-musl-arm.so.1 ${IMAGE_ROOTFS}/lib/ld-musl-arm.so.1 
        mv ${IMAGE_ROOTFS}/etc/ohos-ld-musl-arm.path ${IMAGE_ROOTFS}/etc/ld-musl-arm.path 
    fi
}

ROOTFS_POSTPROCESS_COMMAND += "use_ohos_musl; "
