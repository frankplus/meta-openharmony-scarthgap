# SPDX-FileCopyrightText: Huawei Inc.
#
# SPDX-License-Identifier: Apache-2.0

# This is needed for the STAGING_LIBDIR_JVM_NATIVE variable used below
inherit java

# Make sure we have java tools in $PATH
DEPENDS:append = " openjdk-8-native"
JAVA_BUILD_VERSION = "openjdk-8-native"
export JAVA_HOME = "${STAGING_LIBDIR_JVM_NATIVE}/${JAVA_BUILD_VERSION}"
export CLASSPATH += "${STAGING_LIBDIR_JVM_NATIVE}/${JAVA_BUILD_VERSION}/lib/tools.jar"
EXTRANATIVEPATH:append = " ../${baselib}/jvm/${JAVA_BUILD_VERSION}/bin"
