DESCRIPTION="Hello Test Program"
SECTION="sinby/app"
LICENSE="MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
#FILESYSTEM_PERMS_TABLES := "files/sby-fs-perms.txt"
my_sbindir="/homex"
srcdir="/usr/src"
#mandir="/gegebo"
#bindir="/memem"

#EXTRA_OEMAKE += "default"
#EXTRA_OEMAKE += "make-error"

SRC_URI="file:///tmp/hello.tar.gz"
MY_VALUE ??= "my-value hello test"
#S="${WORKDIR}"
do_compile[dirs] = "${WORKDIR}"
do_install[dirs] = "${WORKDIR}"
#INSANE_SKIP_hello-test="installed-vs-shipped"

do_fetch_prepend () {
    bb.warn("warn:" + d.getVar('PN', True) + ":" + d.getVar('WORKDIR', True) + d.getVar('S', True))
}

do_compile_prepend() {
    ${CC} hello.c -o hello
    printf "compile ${EXTRA_OEMAKE}\n"
    printf "PACKAGES ${PACKAGES}\n"
}

do_package_prepend () {
    def get_fs_perms_list(d):
        str = ""
        bbpath = d.getVar('BBPATH', True)
        fs_perms_tables = d.getVar('FILESYSTEM_PERMS_TABLES', True)
        if not fs_perms_tables:
            fs_perms_tables = 'files/fs-perms.txt'
        for conf_file in fs_perms_tables.split():
            str += " %s" % bb.utils.which(bbpath, conf_file)
            bb.warn("conf_file:%s %s" % (conf_file, str))
        return str

    bb.warn("warn:" + d.getVar('PN', True) + ":" + d.getVar('WORKDIR', True) + ":" + d.getVar('PACKAGES', True))
    bb.warn("warn:" + "  /  " + d.getVar('BBPATH', True))
    bb.warn("xwarn:" + "  /  " + get_fs_perms_list(d))
    bb.warn("bindir = %s" % d.getVar('bindir', True))

    dvar = d.getVar('PKGD', True)
    bb.warn("bindir = %s %s" % (dvar, d.getVar('PACKAGES', True)))
}

do_install_prepend () {
    printf "Hello ${PR}\n" > ${WORKDIR}/dip
}

do_install_append () {
    printf "Hello ${PR}\n" > ${WORKDIR}/dia

   # install -d ${D}/$tmp
# install -m 0644 ${WORKDIR}/dia ${D}/dia

    install -d ${D}${bindir}
    install -m 0755 hello ${D}${bindir}

    install -d ${D}${datadir}
    install -m 0444 ${WORKDIR}/dia ${D}${datadir}/dia
    install -m 0444 ${WORKDIR}/hello.c ${D}${datadir}/hello.c
    install -m 0444 ${WORKDIR}/dip ${D}${datadir}/dip
    printf "datadir ${datadir}\n"

    install -d ${D}/${my_sbindir}
    install -m 0444 ${WORKDIR}/dia ${D}/${my_sbindir}/dia
    printf "my_sbindir ${my_sbindir}\n"
}

addtask hello_world before do_configure after do_patch

do_hello_world () {
    printf "hello world ${PR}:${MY_VALUE}:${SRC_URI}\n"
    printf "${FILESYSTEM_PERMS_TABLES}\n"
}
do_hello_world[doc]="Hello World Script Sample"

