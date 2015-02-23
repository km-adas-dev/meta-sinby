DESCRIPTION="Make boot.bin for Zynq"
SECTION="sinby/bsp"
LICENSE="MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

FSBL_ZYNQ ?= "file://fsbl_zynq.elf"
ZYNQ_BIT_STREAM ?= "file://system.bit"
U_BOOT_FOR_BOOT_BIN ?= "u-boot-xlnx"
inherit deploy

do_deploy[dirs] += "${WORKDIR}"
#do_deploy[depends] += "u-boot-xlnx:do_deploy"
RDEPENDS_${PN} += "${U_BOOT_FOR_BOOT_BIN}"
FSBL_ZYNQ_PATH ?= "${TOPDIR}/files"

#FILESEXTRAPATHS_append = 'please append your build zynq directory'
FILESEXTRAPATHS_append = "${FSBL_ZYNQ_PATH}"

SRC_URI = "${FSBL_ZYNQ} ${ZYNQ_BIT_STREAM}"
addtask do_deploy before do_build after do_compile
#addtask my_task after do_build

python do_deploy () {
    import os
    from py_bootgen.bootgen import BootGen

    boot_bin = "boot.bin"

    fd = open(boot_bin, "wb")

    bootgen = BootGen(fd)

    fsbl_zynq_elf_bin = "fsbl_zynq.elf" + ".bin"
    bit_file_bin = "system.bit" + ".bin"
    uboot_elf_bin = "u-boot.elf" + ".bin"
    
    rv = os.system("arm-none-linux-gnueabi-objcopy -O binary %s %s" % ("fsbl_zynq.elf", fsbl_zynq_elf_bin))
    rv = bootgen.strip_bit("system.bit", bit_file_bin)

    rv = os.system("arm-none-linux-gnueabi-objcopy -O binary %s %s" % ("u-boot.elf", uboot_elf_bin))

    bootgen.make_boot_bin("fsbl_zynq.elf", fsbl_zynq_elf_bin, "system.bit", bit_file_bin, "u-boot.elf", uboot_elf_bin)

    fd.close()
}

#do_my_task () {
#	bbwarn "o ryos comment"
#}

