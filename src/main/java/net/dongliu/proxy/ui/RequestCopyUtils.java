package net.dongliu.proxy.ui;

import net.dongliu.proxy.data.*;
import net.dongliu.proxy.store.Body;
import net.dongliu.proxy.store.BodyType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static net.dongliu.proxy.utils.Headers.headerSet;
import static net.dongliu.proxy.utils.NameValues.parseUrlEncodedParams;

/**
 * Utils for copy request
 */
public class RequestCopyUtils {

    public static final Set<String> filterRequestHeaders = headerSet(
            "Host",
            "Content-Length",
            "Transfer-Encoding",
            "Accept-Encoding",
            "Connection"
    );

    public static void copyRequestAsCurl(HttpMessage httpMessage) {
        copyRequestWith(httpMessage, new RequestTransformer() {
            @Override
            public void onRequestBegin(StringBuilder sb, String method, String url) {
                sb.append("curl -X").append(method).append(" '").append(httpMessage.url()).append("'");
            }

            @Override
            public void onHeaders(StringBuilder sb, List<Header> headers) {
                for (Header header : headers) {
                    sb.append(" \\\n\t ").append("-H'").append(header.rawHeader()).append("'");
                }
            }

            @Override
            public void onBody(StringBuilder sb, ReqBody reqBody) {
                switch (reqBody.type) {
                    case ReqBody.TYPE_FORM:
                        List<? extends NameValue> params = reqBody.asForm();
                        //TODO: what if value contains '&'
                        var forms = params.stream().map(nv -> nv.name() + "=" + nv.value())
                                .collect(joining("&"));
                        sb.append(" \\\n\t --data-raw '").append(forms).append("'");
                        break;
                    case ReqBody.TYPE_TEXT:
                        sb.append(" \\\n\t --data-raw '").append(reqBody.asText()).append("'");
                        break;
                    case ReqBody.TYPE_BINARY:
                        sb.append(" \\\n\t --data-binary '@").append("{your_body_file}").append("'");
                        break;
                }
            }

        });
    }

    public static void copyRequestAsPython(HttpMessage httpMessage) {
        copyRequestWith(httpMessage, new RequestTransformer() {
            @Override
            public void onRequestBegin(StringBuilder sb, String method, String url) {
                sb.append("# install requests with pip install requests\n");
                sb.append("# import requests\n");
                sb.append("response = requests.").append(method.toLowerCase()).append("(")
                        .append(toPyStr(url));
            }

            @Override
            public void onHeaders(StringBuilder sb, List<Header> headers) {
                if (headers.isEmpty()) {
                    return;
                }
                sb.append(", headers = {\n");
                for (Header header : headers) {
                    sb.append("    ").append(toPyStr(header.name()))
                            .append(": ").append(toPyStr(header.value())).append(",\n");
                }
                sb.append("}");
            }

            @Override
            public void onBody(StringBuilder sb, ReqBody reqBody) {
                switch (reqBody.type) {
                    case ReqBody.TYPE_FORM:
                        sb.append(", data=(\n");
                        var forms = reqBody.asForm();
                        for (NameValue param : forms) {
                            sb.append("    (")
                                    .append(toPyStr(param.name())).append(", ")
                                    .append(toPyStr(param.value())).append("),\n");
                        }
                        sb.append(")");
                        break;
                    case ReqBody.TYPE_TEXT:
                        sb.append(", data=").append(toPyStr(reqBody.asText()));
                        break;
                    case ReqBody.TYPE_BINARY:
                        sb.append(", data=").append("{your_body_file}");
                        break;
                }
            }

            @Override
            public void onEnd(StringBuilder sb) {
                sb.append(")\n");
            }
        });
    }

    public static void copyRequestAsJava(HttpMessage httpMessage) {
        copyRequestWith(httpMessage, new RequestTransformer() {
            @Override
            public void onRequestBegin(StringBuilder sb, String method, String url) {
                sb.append("//add dependencies:\n" +
                        "//<dependency>\n" +
                        "//    <groupId>net.dongliu</groupId>\n" +
                        "//    <artifactId>requests</artifactId>\n" +
                        "//    <version>4.18.1</version>\n" +
                        "//</dependency>\n");
                sb.append("\n//import net.dongliu.requests.Parameter;\n" +
                        "//import net.dongliu.requests.Requests;\n" +
                        "//import net.dongliu.requests.Response;\n");

                sb.append("\nResponse<String> response = Requests.")
                        .append(method.toLowerCase()).append("(").append(toJavaStr(url)).append(")");
            }

            @Override
            public void onHeaders(StringBuilder sb, List<Header> headers) {
                if (headers.isEmpty()) {
                    return;
                }
                sb.append(".headers(\n");
                int count = 0;
                for (Header header : headers) {
                    String name = header.name();
                    sb.append("        Parameter.of(").append(toJavaStr(name))
                            .append(", ").append(toJavaStr(header.value())).append(")");
                    if (++count < headers.size()) {
                        sb.append(",\n");
                    } else {
                        sb.append("\n");
                    }
                }
                sb.append("    )");
            }

            @Override
            public void onBody(StringBuilder sb, ReqBody reqBody) {
                switch (reqBody.type) {
                    case ReqBody.TYPE_FORM:
                        sb.append(".body(\n");
                        var params = reqBody.asForm();
                        for (int i = 0; i < params.size(); i++) {
                            var param = params.get(i);
                            sb.append("        Parameter.of(")
                                    .append(toJavaStr(param.name())).append(", ")
                                    .append(toJavaStr(param.value())).append(")");
                            if (i < params.size() - 1) {
                                sb.append(",\n");
                            } else {
                                sb.append("\n");
                            }
                        }
                        sb.append("    )");
                        break;
                    case ReqBody.TYPE_TEXT:
                        sb.append("    .body(").append(toJavaStr(reqBody.asText())).append(")\n");
                        break;
                    case ReqBody.TYPE_BINARY:
                        sb.append("    .body(").append("{your_body_file})\n");
                        break;
                }
            }

            @Override
            public void onEnd(StringBuilder sb) {
                sb.append(".send().toTextResponse();");
            }
        });
    }


    private static void copyRequestWith(HttpMessage httpMessage, RequestTransformer transformer) {
        StringBuilder sb = new StringBuilder();
        HttpHeaders httpHeaders = httpMessage.requestHeader();
        String method = ((HttpRequestHeaders) httpHeaders).method();
        transformer.onRequestBegin(sb, method, httpMessage.url());
        List<Header> headers = httpHeaders.headers().stream()
                .filter(h -> !h.name().isEmpty())
                .filter(h -> h.name().charAt(0) != ':') //http2 pseudo-header
                .filter(h -> !filterRequestHeaders.contains(h.name())) //header set by http tools
                .map(h -> new Header(toHttp1HeaderName(h.name()), h.value())) // http2 names to http1 names
                .collect(toList());
        transformer.onHeaders(sb, headers);
        Body body = httpMessage.requestBody();

        if (body.size() > 0) {
            ReqBody reqBody;
            if (body.type().isText()) {
                String text = body.getAsString();
                if (body.type() == BodyType.www_form) {
                    var params = parseUrlEncodedParams(text, body.charset().orElse(StandardCharsets.UTF_8));
                    reqBody = new ReqBody(ReqBody.TYPE_FORM, params);
                } else {
                    reqBody = new ReqBody(ReqBody.TYPE_TEXT, text);
                }
            } else {
                reqBody = new ReqBody(ReqBody.TYPE_BINARY, body);
            }
            transformer.onBody(sb, reqBody);
        }
        transformer.onEnd(sb);
        UIUtils.copyToClipBoard(sb.toString());
    }

    private static String toHttp1HeaderName(String name) {
        if (!Character.isLowerCase(name.charAt(0))) {
            return name;
        }
        char[] chars = name.toCharArray();
        boolean toUpper = true;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (toUpper) {
                chars[i] = Character.toUpperCase(c);
                toUpper = false;
            }
            if (c == '-') {
                toUpper = true;
            }
        }
        return new String(chars);
    }

    private static String toJavaStr(String str) {
        str = str.replace("\\", "\\\\");
        str = str.replace("\"", "\\\"");
        return "\"" + str + "\"";
    }

    private static String toPyStr(String str) {
        boolean raw = false;
        if (str.contains("\\")) {
            raw = true;
        }
        String del;
        if (!str.contains("'")) {
            del = "'";
        } else if (!str.contains("\"")) {
            del = "\"";
        } else if (!str.contains("'''")) {
            //
            del = "'''";
        } else if (!str.contains("\"\"\"")) {
            //
            del = "\"\"\"";
        } else {
            raw = false;
            del = "'";
            str = str.replace("\\", "\\\\");
            str = str.replace("'", "\\'");
        }

        var sb = new StringBuilder();
        if (raw) {
            sb.append("r");
        }
        sb.append(del).append(str).append(del);
        return sb.toString();
    }

    private interface RequestTransformer {
        void onRequestBegin(StringBuilder sb, String method, String url);

        void onHeaders(StringBuilder sb, List<Header> headers);

        void onBody(StringBuilder sb, ReqBody body);

        default void onEnd(StringBuilder sb) {
        }
    }

    private static class ReqBody {
        private final int type;
        private final static int TYPE_BINARY = 0;
        private final static int TYPE_TEXT = 1;
        private final static int TYPE_FORM = 2;
        //TODO: Multipart form type
        private final Object value;

        public ReqBody(int type, Object value) {
            this.type = type;
            this.value = requireNonNull(value);
        }

        @SuppressWarnings("unchecked")
        public List<? extends NameValue> asForm() {
            return (List<? extends NameValue>) this.value;
        }

        public String asText() {
            return (String) this.value;
        }
    }
}
