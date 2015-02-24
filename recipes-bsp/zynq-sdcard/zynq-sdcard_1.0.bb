DESCRIPTION="Make sdcard for Zynq"
SECTION="sinby/bsp"
LICENSE="MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

FILES_IN_DEPLOY_DIR_IMAGE ?= "${ZYNQ_BOOT_BIN} ${ZYNQ_SDCARD_UIMAGE} ${ZYNQ_SDCARD_DEVICETREE_DTB} ${ZYNQ_SDCARD_URAMDISK}"
ZYNQ_SDCARD_UIMAGE ?= "uImage"
ZYNQ_SDCARD_DEVICETREE_DTB ?= "devicetree.dtb"
#ZYNQ_SDCARD_URAMDISK ?= "uramdisk.tar.gz"

FILES_IN_LOCAL_DIR ?= ""
#RDEPENDS = "zynq-boot-bin linux-xlnx parted-native dosfstools-native mtools-native"

inherit deploy

do_deploy[dirs] += "${WORKDIR}"
ZYNQ_FILES_PATH ?= "${TOPDIR}/files"

#FILESEXTRAPATHS_append = 'please append your build zynq directory'
FILESEXTRAPATHS_append = "${ZYNQ_FILES_PATH}"

#SRC_URI = "${ZYNQ_FSBL_ZYNQ} ${ZYNQ_SYSTEM_BIT}"
addtask do_deploy before do_build after do_compile
ZYNQ_BOOT_SDCARD ?= "zynq_boot.sdcard"
ZYNQ_BOOT_RESERVED_SIZE ?= "1"
ZYNQ_BOOT_PARTION1_DOS_FS_SIZE ?= "33292"

do_deploy () {
	echo IMAGE_NAME: ${IMAGE_NAME} >> /tmp/jgeil.txt
	echo DEPLOYDIR:${DEPLOY_DIR_IMAGE} >> /tmp/jgeil.txt
	ROOTFS_SIZE=`du ${DEPLOY_DIR_IMAGE}/core-image-minimal-zc702-zynq7.ext4`
	echo ROOTFS_SIZE: $ROOTFS_SIZE >> /tmp/jgeil.txt
	ROOTFS_SIZE=12288
	ZYNQ_BOOT_PARTION2_FS_SIZE_BYTE=$(expr $ROOTFS_SIZE + 1023)
	Z=$(expr $ZYNQ_BOOT_PARTION2_FS_SIZE_BYTE \/ 512)
	echo 1st:$ZYNQ_BOOT_PARTION2_FS_SIZE_BYTE >> /tmp/jgeil.txt
	#Z=$(expr $ZYNQ_BOOT_PARTION2_FS_SIZE_BYTE \/ 1024)
	echo $Z >> /tmp/jgeil.txt
}

