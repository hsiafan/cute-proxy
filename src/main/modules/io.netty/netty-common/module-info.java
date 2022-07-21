open module io.netty.common {
    requires static commons.logging;
    requires static java.logging;
    requires static log4j;
    requires static log4j.api;
    requires static org.slf4j;

    requires transitive jdk.unsupported;

    exports io.netty.util;
    exports io.netty.util.collection;
    exports io.netty.util.concurrent;
    exports io.netty.util.internal;
    exports io.netty.util.internal.logging;
    exports io.netty.util.internal.shaded.org.jctools.queues;
    exports io.netty.util.internal.shaded.org.jctools.queues.atomic;
    exports io.netty.util.internal.shaded.org.jctools.util;

}
