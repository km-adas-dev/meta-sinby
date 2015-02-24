DESCRIPTION="Make sdcard for Zynq"
SECTION="sinby/bsp"
LICENSE="MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

FILES_IN_DEPLOY_DIR_IMAGE ?= "${ZYNQ_BOOT_BIN} ${ZYNQ_SDCARD_UIMAGE} ${ZYNQ_SDCARD_DEVICETREE_DTB} ${ZYNQ_SDCARD_URAMDISK}"
ZYNQ_SDCARD_UIMAGE ?= "uImage"
ZYNQ_SDCARD_DEVICETREE_DTB ?= "devicetree.dtb"
#ZYNQ_SDCARD_URAMDISK ?= "uramdisk.tar.gz"

FILES_IN_LOCAL_DIR ?= ""
DEPENDS = "zynq-boot-bin linux-xlnx parted-native dosfstools-native mtools-native"

inherit deploy

do_deploy[dirs] += "${WORKDIR}"
ZYNQ_FILES_PATH ?= "${TOPDIR}/files"

#FILESEXTRAPATHS_append = 'please append your build zynq directory'
FILESEXTRAPATHS_append = "${ZYNQ_FILES_PATH}"

#SRC_URI = "${ZYNQ_FSBL_ZYNQ} ${ZYNQ_SYSTEM_BIT}"
addtask do_deploy before do_build after do_compile

python do_deploy () {
    import os
    from pyfat.fat import FAT

}

