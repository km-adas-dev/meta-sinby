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

    boot_bin = d.expand('${DEPLOYDIR}/${ZYNQ_BOOT_BIN}')
    #bb.warn("boot_bin is %s" %  boot_bin)
    #boot_bin = "${ZYNQ_BOOT_BIN}"

    #bb.warn("ZYNQ_SYSTEM_BIT %s" % )

    #bb.warn("localpath %s %s" % (d.getVar('ZYNQ_SYSTEM_BIT', True), localpath))

    fd = open(boot_bin, "wb")

    bootgen = BootGen(fd)

    fsbl_zynq_elf_path = os.path.basename(d.expand('${ZYNQ_FSBL_ZYNQ}'))
    fsbl_zynq_elf_bin = fsbl_zynq_elf_path + ".bin"

    system_bit_localpath = bb.fetch2.localpath(d.getVar('ZYNQ_SYSTEM_BIT', True), d)
    system_bit_bin = d.expand("${DEPLOY_DIR_IMAGE}") + "/" + os.path.basename(d.expand("${ZYNQ_SYSTEM_BIT}")) + ".bin"
    #bb.warn("system_bit_bin:%s" % system_bit_bin)
    
    uboot_elf_path = d.expand("${DEPLOY_DIR_IMAGE}/${ZYNQ_APP_ELF}")
    uboot_elf_bin = d.expand("${ZYNQ_APP_ELF}") + ".bin"

    objcopy = d.expand("${HOST_PREFIX}objcopy")
    cmd = "%s -O binary %s %s" % (objcopy, fsbl_zynq_elf_path, fsbl_zynq_elf_bin)
    (retval, output) = oe.utils.getstatusoutput(cmd)
    if retval:
        bb.fatal("objcopy failed with exit code %s (cmd is %s)%s" % (retval, cmd, ":\n%s" % output if output else ""))

    rv = bootgen.strip_bit(system_bit_localpath, system_bit_bin)
    if rv == False :
        bb.fatal("bootgen.strip_bit failed from %s to %s" % (system_bit_localpath, system_bit_bin))
        return

    cmd = "%s -O binary %s %s" % (objcopy, uboot_elf_path, uboot_elf_bin)
    (retval, output) = oe.utils.getstatusoutput(cmd)
    if retval:
        bb.fatal("objcopy failed with exit code %s (cmd is %s)%s" % (retval, cmd, ":\n%s" % output if output else ""))

    start_address = bootgen.get_start_address(uboot_elf_path)
    # u-boot.elf : start_address = 0x04000000
    # app.elf    : start_address = 0x00100000

    bootgen.make_boot_bin(fsbl_zynq_elf_path, fsbl_zynq_elf_bin, system_bit_localpath, system_bit_bin, uboot_elf_path, uboot_elf_bin, start_address)

    fd.close()

    #install ${ZYNQ_BOOT_BIN} ${DEPLOYDIR}/${ZYNQ_BOOT_BIN}
}

#do_my_task () {
#	bbwarn "o ryos comment"
#}

