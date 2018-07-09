package net.dongliu.proxy.ui;

import net.dongliu.commons.collection.Lists;
import net.dongliu.commons.io.Readers;
import net.dongliu.proxy.data.*;
import net.dongliu.proxy.store.Body;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static net.dongliu.proxy.utils.Headers.headerSet;

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
            public void onBody(StringBuilder sb, TheBody theBody) {
                if (theBody.type == TheBody.TYPE_TEXT) {
                    sb.append(" \\\n\t -d'").append(theBody.asText()).append("'");
                } else if (theBody.type == TheBody.TYPE_BINARY) {
                    sb.append(" \\\n\t -d'@").append("{your_body_file}").append("'");
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
                sb.append("response = requests.").append(method.toLowerCase()).append("(r'").append(url).append("'");
            }

            @Override
            public void onHeaders(StringBuilder sb, List<Header> headers) {
                if (headers.isEmpty()) {
                    return;
                }
                sb.append(", headers = {\n");
                for (Header header : headers) {
                    sb.append("    r'").append(header.name()).append("': r'").append(header.value()).append("',\n");
                }
                sb.append("}");
            }

            @Override
            public void onBody(StringBuilder sb, TheBody theBody) {
                switch (theBody.type) {
                    case TheBody.TYPE_TEXT:
                        sb.append(", data=r'").append(theBody.asText()).append("'");
                        break;
                    case TheBody.TYPE_BINARY:
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
                        .append(method.toLowerCase()).append("(\"").append(url.replace("\"", "\\\"")).append("\")");
            }

            @Override
            public void onHeaders(StringBuilder sb, List<Header> headers) {
                if (headers.isEmpty()) {
                    return;
                }
                sb.append(".headers(\n");
                int count = 0;
                for (Header header : headers) {
                    sb.append("Parameter.of(\"").append(header.name()).append("\", \"").append(header.value());
                    if (++count < headers.size()) {
                        sb.append("\"),\n");
                    } else {
                        sb.append("\")\n");
                    }
                }
                sb.append(")");
            }

            @Override
            public void onBody(StringBuilder sb, TheBody theBody) {
                switch (theBody.type) {
                    case TheBody.TYPE_TEXT:
                        sb.append(", data=r'").append(theBody.asText()).append("'");
                        break;
                    case TheBody.TYPE_BINARY:
                        sb.append(", data=").append("{your_body_file}");
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
        List<Header> headers = httpHeaders.headers();
        headers = Lists.filter(headers, h -> !filterRequestHeaders.contains(h.name()));
        transformer.onHeaders(sb, headers);
        Body body = httpMessage.requestBody();

        if (body.size() > 0) {
            TheBody theBody;
            if (body.type().isText()) {
                String text;
                try (var input = body.getDecodedInputStream();
                     var reader = new InputStreamReader(input, body.charset().orElse(UTF_8))) {
                    text = Readers.readAll(reader);
                    theBody = new TheBody(TheBody.TYPE_TEXT, text);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                //TODO: binary body
                theBody = new TheBody(TheBody.TYPE_BINARY, body);
            }
            transformer.onBody(sb, theBody);
        }
        transformer.onEnd(sb);
        UIUtils.copyToClipBoard(sb.toString());
    }

    private interface RequestTransformer {
        void onRequestBegin(StringBuilder sb, String method, String url);

        void onHeaders(StringBuilder sb, List<Header> headers);

        void onBody(StringBuilder sb, TheBody body);

        default void onEnd(StringBuilder sb) {
        }
    }

    private static class TheBody {
        private final int type;
        private final static int TYPE_BINARY = 0;
        private final static int TYPE_TEXT = 1;
        private final static int TYPE_FORM = 2;
        private final Object value;

        public TheBody(int type, Object value) {
            this.type = type;
            this.value = requireNonNull(value);
        }

        @SuppressWarnings("unchecked")
        public List<NameValue> asForm() {
            return (List<NameValue>) this.value;
        }

        public String asText() {
            return (String) this.value;
        }
    }
}
