DESCRIPTION="Make boot.bin for Zynq"
SECTION="sinby/bsp"
LICENSE="MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

#S="${WORKDIR}"
do_deploy[dirs] += "${WORKDIR}"
#do_deploy[dirs] = "${S}"
#addtask deploy before do_build after do_compile
addtask deploy after do_build 

SRC_URI = "file://u-boot.elf"
#SRC_URI += "${MY_ELF}"
MY_ELF ?= "file://zynq_fsbl.elf"
#FILESEXTRAPATHS_append = "/tmp"
#FILESEXTRAPATHS_append = "${BUILDDIR}"
#FILESEXTRAPATHS_append = "${@os.environ["BUILDDIR"]}"
FILESEXTRAPATHS_append = 'ls /tmp'
#XOOT_KERNEL_DEVICETREE ?= "${@expand_dir_basepaths_by_extension(os.environ["BUILDDIR"])}"
#XOOT_KERNEL_DEVICETREE ?= "${@expand_dir_basepaths_by_extension("BUILDDIR")}"

python do_deploy () {
    bb.warn("ryos warn:" + d.getVar('PN', True) + ":" + d.getVar('WORKDIR', True) + ":" + d.getVar('S', True) + ":" + d.getVar('MY_ELF', True) + ":" + d.getVar('FILESEXTRAPATHS', True) + ":" + d.getVar('THISDIR', True) + ":" + d.getVar('BB_CURRENTTASK', True) + ":" )
}

do_deploy[postfuncs] += "xxo_deploy_xppend"

xxo_deploy_xppend () {
    #bb.warn("jjjjjj warn:" + d.getVar('PN', True) + ":" + d.getVar('WORKDIR', True) + d.getVar('S', True))

   # install -d ${D}/$tmp
# install -m 0644 ${WORKDIR}/dia ${D}/dia

#    install -d ${D}${bindir}
#    install -m 0755 hello ${D}${bindir}

#    install -d ${D}${datadir}
#    install -m 0444 ${WORKDIR}/dia ${D}${datadir}/dia
#    install -m 0444 ${WORKDIR}/hello.c ${D}${datadir}/hello.c
#    install -m 0444 ${WORKDIR}/dip ${D}${datadir}/dip
    printf "datadir ${datadir}\n"

#    install -d ${D}/${my_sbindir}
#    install -m 0444 ${WORKDIR}/dia ${D}/${my_sbindir}/dia
#    printf "my_sbindir ${my_sbindir}\n"
}

#import os

def expand_dir_basepaths_by_extension(dir):
    #return os.environ['BUILDDIR'];
    return os.environ.get("ryos")

def my_find_bblayers(dir):
    """
    Find and return a sanitized list of the layers found in BBLAYERS.
    """
    builddir = os.environ["xBUILDDIR"]
    bblayers_conf = os.path.join(builddir, "conf/bblayers.conf")
    return bblayers_conf

