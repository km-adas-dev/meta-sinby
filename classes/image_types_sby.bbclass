inherit image_types

IMAGE_BOOTLOADER ?= "u-boot-xlnx"

# Handle u-boot suffixes
UBOOT_SUFFIX ?= "bin"
UBOOT_PADDING ?= "0"
UBOOT_SUFFIX_SDCARD ?= "${UBOOT_SUFFIX}"

#
# Handles i.MX mxs bootstream generation
#

# U-Boot mxsboot generation to SD-Card
# Boot partition size [in KiB]
BOOT_SPACE ?= "8192"

# Set alignment to 4MB [in KiB]
IMAGE_ROOTFS_ALIGNMENT = "4096"

IMAGE_DEPENDS_sdcard = "parted-native dosfstools-native mtools-native \
                        virtual/kernel ${IMAGE_BOOTLOADER} linux-xlnx"

SDCARD = "${DEPLOY_DIR_IMAGE}/${IMAGE_NAME}.rootfs.sdcard"

SDCARD_GENERATION_COMMAND_zynq7 = "generate_zynq7_sdcard"

#
# Create an image that can by written onto a SD card using dd for use
# with i.MX SoC family
#
# External variables needed:
#   ${SDCARD_ROOTFS}    - the rootfs image to incorporate
#   ${IMAGE_BOOTLOADER} - bootloader to use {u-boot, barebox}
#
# The disk layout used is:
#
#    0                      -> IMAGE_ROOTFS_ALIGNMENT         - reserved to bootloader (not partitioned)
#    IMAGE_ROOTFS_ALIGNMENT -> BOOT_SPACE                     - kernel and other data
#    BOOT_SPACE             -> SDIMG_SIZE                     - rootfs
#
#                                                     Default Free space = 1.3x
#                                                     Use IMAGE_OVERHEAD_FACTOR to add more space
#                                                     <--------->
#            4MiB               8MiB           SDIMG_ROOTFS                    4MiB
# <-----------------------> <----------> <----------------------> <------------------------------>
#  ------------------------ ------------ ------------------------ -------------------------------
# | IMAGE_ROOTFS_ALIGNMENT | BOOT_SPACE | ROOTFS_SIZE            |     IMAGE_ROOTFS_ALIGNMENT    |
#  ------------------------ ------------ ------------------------ -------------------------------
# ^                        ^            ^                        ^                               ^
# |                        |            |                        |                               |
# 0                      4096     4MiB +  8MiB       4MiB +  8Mib + SDIMG_ROOTFS   4MiB +  8MiB + SDIMG_ROOTFS + 4MiB
generate_zynq7_sdcard () {
	# Create partition table
	parted -s ${SDCARD} mklabel msdos
	parted -s ${SDCARD} unit KiB mkpart primary fat32 ${IMAGE_ROOTFS_ALIGNMENT} $(expr ${IMAGE_ROOTFS_ALIGNMENT} \+ ${BOOT_SPACE_ALIGNED})
	parted -s ${SDCARD} unit KiB mkpart primary $(expr  ${IMAGE_ROOTFS_ALIGNMENT} \+ ${BOOT_SPACE_ALIGNED}) $(expr ${IMAGE_ROOTFS_ALIGNMENT} \+ ${BOOT_SPACE_ALIGNED} \+ $ROOTFS_SIZE)
	parted ${SDCARD} print

	# Burn bootloader
	case "${IMAGE_BOOTLOADER}" in
		imx-bootlets)
		bberror "The imx-bootlets is not supported for i.MX based machines"
		exit 1
		;;
		u-boot-xlnx)
		dd if=${DEPLOY_DIR_IMAGE}/u-boot-${MACHINE}.${UBOOT_SUFFIX_SDCARD} of=${SDCARD} conv=notrunc seek=2 skip=${UBOOT_PADDING} bs=512
		;;
		barebox)
		dd if=${DEPLOY_DIR_IMAGE}/barebox-${MACHINE}.bin of=${SDCARD} conv=notrunc seek=1 skip=1 bs=512
		dd if=${DEPLOY_DIR_IMAGE}/bareboxenv-${MACHINE}.bin of=${SDCARD} conv=notrunc seek=1 bs=512k
		;;
		"")
		;;
		*)
		bberror "Unkown IMAGE_BOOTLOADER value"
		exit 1
		;;
	esac

	# Create boot partition image
	BOOT_BLOCKS=$(LC_ALL=C parted -s ${SDCARD} unit b print \
	                  | awk '/ 1 / { print substr($4, 1, length($4 -1)) / 1024 }')
	mkfs.vfat -n "${BOOTDD_VOLUME_ID}" -S 512 -C ${WORKDIR}/boot.img $BOOT_BLOCKS
	mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-${MACHINE}.bin ::/${KERNEL_IMAGETYPE}

	# Copy boot scripts
	for item in ${BOOT_SCRIPTS}; do
		src=`echo $item | awk -F':' '{ print $1 }'`
		dst=`echo $item | awk -F':' '{ print $2 }'`

		mcopy -i ${WORKDIR}/boot.img -s $src ::/$dst
	done

	# Copy device tree file
	if test -n "${KERNEL_DEVICETREE}"; then
		for DTS_FILE in ${KERNEL_DEVICETREE}; do
			DTS_BASE_NAME=`basename ${DTS_FILE} | awk -F "." '{print $1}'`
			if [ -e "${KERNEL_IMAGETYPE}-${DTS_BASE_NAME}.dtb" ]; then
				kernel_bin="`readlink ${KERNEL_IMAGETYPE}-${MACHINE}.bin`"
				kernel_bin_for_dtb="`readlink ${KERNEL_IMAGETYPE}-${DTS_BASE_NAME}.dtb | sed "s,$DTS_BASE_NAME,${MACHINE},g;s,\.dtb$,.bin,g"`"
				if [ $kernel_bin = $kernel_bin_for_dtb ]; then
					mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-${DTS_BASE_NAME}.dtb ::/${DTS_BASE_NAME}.dtb
				fi
			fi
		done
	fi

	# Burn Partition
	dd if=${WORKDIR}/boot.img of=${SDCARD} conv=notrunc seek=1 bs=$(expr ${IMAGE_ROOTFS_ALIGNMENT} \* 1024) && sync && sync
	dd if=${SDCARD_ROOTFS} of=${SDCARD} conv=notrunc seek=1 bs=$(expr ${BOOT_SPACE_ALIGNED} \* 1024 + ${IMAGE_ROOTFS_ALIGNMENT} \* 1024) && sync && sync
}

IMAGE_CMD_sdcardy () {
}

IMAGE_CMD_sdcard () {
	echo ${BB_CURRENTTASK} >> /tmp/tas.tk
	if [ -z "${SDCARD_ROOTFS}" ]; then
		bberror "SDCARD_ROOTFS is undefined. To use sdcard image from Freescale's BSP it needs to be defined."
		exit 1
	fi

	# Align boot partition and calculate total SD card image size
	BOOT_SPACE_ALIGNED=$(expr ${BOOT_SPACE} + ${IMAGE_ROOTFS_ALIGNMENT} - 1)
	BOOT_SPACE_ALIGNED=$(expr ${BOOT_SPACE_ALIGNED} - ${BOOT_SPACE_ALIGNED} % ${IMAGE_ROOTFS_ALIGNMENT})
	SDCARD_SIZE=$(expr ${IMAGE_ROOTFS_ALIGNMENT} + ${BOOT_SPACE_ALIGNED} + $ROOTFS_SIZE + ${IMAGE_ROOTFS_ALIGNMENT})

	# Initialize a sparse file
	dd if=/dev/zero of=${SDCARD} bs=1 count=0 seek=$(expr 1024 \* ${SDCARD_SIZE})

#	${SDCARD_GENERATION_COMMAND}
}

do_rootfs[prefuncs] += "make_zynq_dos_image"

FSBL_ELF ?= "fsbl_${MACHINE}.elf"
#FSBL_ELF ?= fsbl_zc702_zynq7.elf

python make_zynq_dos_image() {
	from pyfat.fat import FAT
	from py_bootgen.bootgen import BootGen
	import os

	#rv = os.system("${HOST_PREFIX}objcopy -O binary ${FSBL_ELF} ${FSBL_ELF}.bin")
	rv = os.system("${HOST_PREFIX}objcopy -O binary /tmp/zynq_fsbl.elf /tmp/zynq_fsbl.elf.bin")
	if rv != 0 :
		return rv
	#rv = bootgen.strip_bit(bit_file, bit_file_bin)
}

#addtask sinby_test before do_configure
do_sinby_test() {
}

inherit xilinx-utils

SBY_ZYNQ_SDCARD_TMP ?= "${WORKDIR}/zynq_sdcard_tmp"
MACHINE_DEVICETREE ?= "zc702_devicetree_hdmi.dts"
OOT_KERNEL_DEVICETREE ?= "${@expand_dir_basepaths_by_extension("MACHINE_DEVICETREE", os.path.join(d.getVar("WORKDIR", True), 'devicetree'), '.dts', d)}"

make_zynq_dos_image_sh() {
	echo xmake_zynq_dos_image_sh:${HOST_PREFIX}objcopy -O binary ${FSBL_ELF} ${FSBL_ELF}.bin > /tmp/jgeil.txt
	if [ ! -e ${SBY_ZYNQ_SDCARD_TMP} ] ; then
		mkdir ${SBY_ZYNQ_SDCARD_TMP}
	fi
	pwd >> /tmp/jgeil.txt
	echo OOT_KERNEL_DEVICETREE:${OOT_KERNEL_DEVICETREE} >> /tmp/jgeil.txt
	echo BB_CURRENTTASK:${BB_CURRENTTASK} >> /tmp/jgeil.txt
	if test -n "${OOT_KERNEL_DEVICETREE}"; then
		for DTS_FILE in ${OOT_KERNEL_DEVICETREE}; do
			DTS_BASE_NAME=`basename ${DTS_FILE} | awk -F "." '{print $1}'`
			echo DTS_FILE:${DTS_FILE} ${DTS_BASE_NAME} >> /tmp/jgeil.txt
			dtc -I dts -O dtb ${OOT_KERNEL_DEVICETREE_FLAGS} -o ${DTS_BASE_NAME} ${DTS_FILE}
		done
	fi
			
	echo /tmp/zynq* >> /tmp/jgeil.txt
	echo S:${B} >> /tmp/jgeil.txt
	echo B:${S} >> /tmp/jgeil.txt
	echo D:${D} >> /tmp/jgeil.txt
	echo WORKDIR:${WORKDIR} >> /tmp/jgeil.txt
	echo DEPLOY_DIR_IMAGE:${DEPLOY_DIR_IMAGE} >> /tmp/jgeil.txt
}

do_sinby_test[prefuncs] += "make_zynq_dos_image_sh make_zynq_dos_image"
do_rootfs[depends] += "linux-xlnx:do_build"
do_rootfs[depends] += "u-boot-xlnx:do_build"
