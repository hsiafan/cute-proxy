open module bcpkix.jdk15on {
    requires java.naming;

    requires transitive bcprov.jdk15on;

    exports org.bouncycastle.cert;
    exports org.bouncycastle.cert.bc;
    exports org.bouncycastle.cert.cmp;
    exports org.bouncycastle.cert.crmf;
    exports org.bouncycastle.cert.crmf.bc;
    exports org.bouncycastle.cert.crmf.jcajce;
    exports org.bouncycastle.cert.dane;
    exports org.bouncycastle.cert.dane.fetcher;
    exports org.bouncycastle.cert.jcajce;
    exports org.bouncycastle.cert.ocsp;
    exports org.bouncycastle.cert.ocsp.jcajce;
    exports org.bouncycastle.cert.path;
    exports org.bouncycastle.cert.path.validations;
    exports org.bouncycastle.cert.selector;
    exports org.bouncycastle.cert.selector.jcajce;
    exports org.bouncycastle.cms;
    exports org.bouncycastle.cms.bc;
    exports org.bouncycastle.cms.jcajce;
    exports org.bouncycastle.dvcs;
    exports org.bouncycastle.eac;
    exports org.bouncycastle.eac.jcajce;
    exports org.bouncycastle.eac.operator;
    exports org.bouncycastle.eac.operator.jcajce;
    exports org.bouncycastle.mozilla;
    exports org.bouncycastle.mozilla.jcajce;
    exports org.bouncycastle.openssl;
    exports org.bouncycastle.openssl.bc;
    exports org.bouncycastle.openssl.jcajce;
    exports org.bouncycastle.operator;
    exports org.bouncycastle.operator.bc;
    exports org.bouncycastle.operator.jcajce;
    exports org.bouncycastle.pkcs;
    exports org.bouncycastle.pkcs.bc;
    exports org.bouncycastle.pkcs.jcajce;
    exports org.bouncycastle.pkix;
    exports org.bouncycastle.pkix.jcajce;
    exports org.bouncycastle.tsp;
    exports org.bouncycastle.tsp.cms;
    exports org.bouncycastle.voms;

}
