inherit image_types

#
# Handles Zynq bootstream generation
#

# Boot partition size [in KiB]
BOOT_SPACE ?= "8192"

# Set alignment to 4MB [in KiB]
IMAGE_ROOTFS_ALIGNMENT = "4096"

IMAGE_DEPENDS_sdcard = "parted-native dosfstools-native mtools-native \
                        virtual/kernel zynq-boot-bin linux-xlnx"

SDCARD = "${DEPLOY_DIR_IMAGE}/${IMAGE_NAME}.rootfs.sdcard"

MACHINE_DEVICETREE_DTB ?= "zc702_devicetree_hdmi.dtb"
BOOT_BIN ?= "boot.bin"
DEVICETREE_DTB_NAME ?= "devicetree.dtb"

do_rootfs[depends] += "linux-xlnx:do_build"
#do_rootfs[depends] += "u-boot-xlnx"
do_rootfs[depends] += "zynq-boot-bin:do_build"

IMAGE_CMD_sdcard () {
    if [ -z "${SDCARD_ROOTFS}" ]; then
        bberror "SDCARD_ROOTFS is undefined."
        exit 1
    fi
    bbwarn "hello"

    # Align boot partition and calculate total SD card image size
    BOOT_SPACE_ALIGNED=$(expr ${BOOT_SPACE} + ${IMAGE_ROOTFS_ALIGNMENT} - 1)
    BOOT_SPACE_ALIGNED=$(expr ${BOOT_SPACE_ALIGNED} - ${BOOT_SPACE_ALIGNED} % ${IMAGE_ROOTFS_ALIGNMENT})
    SDCARD_SIZE=$(expr ${IMAGE_ROOTFS_ALIGNMENT} + ${BOOT_SPACE_ALIGNED} + $ROOTFS_SIZE + ${IMAGE_ROOTFS_ALIGNMENT})

    # Initialize a sparse file
    dd if=/dev/zero of=${SDCARD} bs=1 count=0 seek=$(expr 1024 \* ${SDCARD_SIZE})
    parted -s ${SDCARD} mklabel msdos
    parted -s ${SDCARD} unit KiB mkpart primary fat32 ${IMAGE_ROOTFS_ALIGNMENT} $(expr ${IMAGE_ROOTFS_ALIGNMENT} \+ ${BOOT_SPACE_ALIGNED})
    parted -s ${SDCARD} unit KiB mkpart primary $(expr  ${IMAGE_ROOTFS_ALIGNMENT} \+ ${BOOT_SPACE_ALIGNED}) $(expr ${IMAGE_ROOTFS_ALIGNMENT} \+ ${BOOT_SPACE_ALIGNED} \+ $ROOTFS_SIZE)
    parted ${SDCARD} print

    BOOT_BLOCKS=$(LC_ALL=C parted -s ${SDCARD} unit b print \
                 | awk '/ 1 / { print substr($4, 1, length($4 -1)) / 1024 }')
    mkfs.vfat -n "${BOOTDD_VOLUME_ID}" -S 512 -C ${WORKDIR}/boot.img $BOOT_BLOCKS
    mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/${BOOT_BIN} ::/${BOOT_BIN}
    mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-${MACHINE}.bin ::/${KERNEL_IMAGETYPE}
    echo mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-${MACHINE}.bin ::/${KERNEL_IMAGETYPE} > /tmp/jgeil.txt
    mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-${MACHINE_DEVICETREE_DTB} ::/${DEVICETREE_DTB_NAME}

    for item in ${BOOT_SCRIPTS}; do
        src=`echo $item | awk -F':' '{ print $1 }'`
        dst=`echo $item | awk -F':' '{ print $2 }'`

        mcopy -i ${WORKDIR}/boot.img -s $src ::/$dst
    done

    dd if=${WORKDIR}/boot.img of=${SDCARD} conv=notrunc seek=1 bs=$(expr ${IMAGE_ROOTFS_ALIGNMENT} \* 1024) && sync && sync
    dd if=${SDCARD_ROOTFS} of=${SDCARD} conv=notrunc seek=1 bs=$(expr ${BOOT_SPACE_ALIGNED} \* 1024 + ${IMAGE_ROOTFS_ALIGNMENT} \* 1024) && sync && sync
}

