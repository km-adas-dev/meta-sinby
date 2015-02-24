DESCRIPTION="Make boot.bin for Zynq"
SECTION="sinby/bsp"
LICENSE="MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

ZYNQ_FSBL_ZYNQ ?= "file://fsbl_zynq.elf"
ZYNQ_SYSTEM_BIT ?= "file://system.bit"
ZYNQ_APP_ELF ?= "u-boot.elf"
ZYNQ_BOOT_BIN ?= "boot.bin"
U_BOOT_FOR_BOOT_BIN ?= "u-boot-xlnx"

inherit deploy

DEPENDS = "u-boot-xlnx virtual/${HOST_PREFIX}binutils"

do_deploy[dirs] += "${WORKDIR}"
#do_deploy[depends] += "u-boot-xlnx:do_deploy"

RDEPENDS_${PN} += "${U_BOOT_FOR_BOOT_BIN}"
ZYNQ_FILES_PATH ?= "${TOPDIR}/files"

#FILESEXTRAPATHS_append = 'please append your build zynq directory'
FILESEXTRAPATHS_append = "${ZYNQ_FILES_PATH}"

SRC_URI = "${ZYNQ_FSBL_ZYNQ} ${ZYNQ_SYSTEM_BIT}"
addtask do_deploy before do_build after do_compile
#addtask my_task after do_build

python do_deploy () {
    import os
    from py_bootgen.bootgen import BootGen

    boot_bin = "${DEPLOY_DIR_IMAGE}/${ZYNQ_BOOT_BIN}"

    fd = open(boot_bin, "wb")

    bootgen = BootGen(fd)

    fsbl_zynq_elf_path = os.path.basename("${ZYNQ_FSBL_ZYNQ}")
    fsbl_zynq_elf_bin = fsbl_zynq_elf_path + ".bin"

    system_bit_path = os.path.basename("${ZYNQ_SYSTEM_BIT}")
    system_bit_bin = system_bit_path + ".bin"

    uboot_elf_path = "${DEPLOY_DIR_IMAGE}/${ZYNQ_APP_ELF}"
    uboot_elf_bin = uboot_elf_path + ".bin"
    
    #rv = os.system("arm-none-linux-gnueabi-objcopy -O binary %s %s" % (fsbl_zynq_elf_path, fsbl_zynq_elf_bin))
    #if rv != 0 :
    #    bb.error("I cannot convert from %s to %s" % (fsbl_zynq_elf_path, fsbl_zynq_elf_bin))
    #    return

    objcopy = "${HOST_PREFIX}objcopy"
    cmd = "%s -O binary %s %s" % (objcopy, fsbl_zynq_elf_path, fsbl_zynq_elf_bin)
    (retval, output) = oe.utils.getstatusoutput(cmd)
    if retval:
        bb.fatal("objcopy failed with exit code %s (cmd was %s)%s" % (retval, cmd, ":\n%s" % output if output else ""))

    rv = bootgen.strip_bit("system.bit", system_bit_bin)
    if rv == False :
        bb.fatal("bootgen.strip_bit failed from %s to %s" % (system_bit_path, system_bit_bin))
        return

    #rv = os.system("arm-none-linux-gnueabi-objcopy -O binary %s %s" % (uboot_elf_path, uboot_elf_bin))
    #if rv != 0 :
    #    bb.error("I cannot convert from %s to %s" % (uboot_elf_path, uboot_elf_bin))
    #    return

    cmd = "%s -O binary %s %s" % (objcopy, uboot_elf_path, uboot_elf_bin)
    (retval, output) = oe.utils.getstatusoutput(cmd)
    if retval:
        bb.fatal("objcopy failed with exit code %s (cmd was %s)%s" % (retval, cmd, ":\n%s" % output if output else ""))

    start_address = bootgen.get_start_address(uboot_elf_path)
    # u-boot.elf : start_address = 0x04000000
    # app.elf    : start_address = 0x00100000

    bootgen.make_boot_bin(fsbl_zynq_elf_path, fsbl_zynq_elf_bin, system_bit_path, system_bit_bin, uboot_elf_path, uboot_elf_bin, start_address)

    fd.close()
}

#do_my_task () {
#	bbwarn "o ryos comment"
#}

