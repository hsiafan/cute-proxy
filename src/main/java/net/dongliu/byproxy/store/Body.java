package net.dongliu.byproxy.store;

import net.dongliu.byproxy.data.ContentType;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.toList;

/**
 * OutputStream impl for storing http body
 *
 * @author Liu Dong
 */
public class Body implements Serializable {
    private static final long serialVersionUID = -5119917294712380816L;
    private static Logger logger = LoggerFactory.getLogger(Body.class);

    private volatile long size;
    // http body has been finished
    private volatile boolean finished;

    private volatile BodyType type;
    private volatile Charset charset;
    private String contentEncoding;

    private List<Chunk> chunkList;

    public Body(BodyType type, @Nullable Charset charset, String contentEncoding) {
        this.type = requireNonNull(type);
        this.charset = requireNonNullElse(charset, UTF_8);
        this.contentEncoding = requireNonNull(contentEncoding);
        this.chunkList = new ArrayList<>();
    }


    public static Body create(ContentType contentType, String contentEncoding) {
        BodyType bodyType;
        bodyType = getHttpBodyType(requireNonNull(contentType));
        return new Body(bodyType, contentType.getCharset(), requireNonNull(contentEncoding));
    }

    private static BodyType getHttpBodyType(ContentType contentType) {
        BodyType bodyType;
        String subType = contentType.getMimeType().getSubType();
        if (contentType.isImage()) {
            if ("png".equals(subType)) {
                bodyType = BodyType.png;
            } else if ("jpeg".equals(subType)) {
                bodyType = BodyType.jpeg;
            } else if ("gif".equals(subType)) {
                bodyType = BodyType.gif;
            } else if ("bmp".equals(subType)) {
                bodyType = BodyType.bmp;
            } else if ("x-icon".equals(subType)) {
                bodyType = BodyType.icon;
            } else {
                bodyType = BodyType.otherImage;
            }
        } else if (contentType.isText()) {
            if ("json".equals(subType)) {
                bodyType = BodyType.json;
            } else if ("html".equals(subType)) {
                bodyType = BodyType.html;
            } else if ("xml".equals(subType)) {
                bodyType = BodyType.xml;
            } else if ("x-www-form-urlencoded".equals(subType)) {
                bodyType = BodyType.www_form;
            } else if ("css".equals(subType)) {
                bodyType = BodyType.css;
            } else if ("javascript".equals(subType) || "x-javascript".equals(subType)) {
                bodyType = BodyType.javascript;
            } else {
                bodyType = BodyType.text;
            }
        } else {
            bodyType = BodyType.binary;
        }
        return bodyType;
    }

    /**
     * Append new data to this body
     */
    public void append(ByteBuffer buffer) {
        int size = buffer.remaining();
        if (size == 0) {
            return;
        }
        DataStoreManager manager = DataStoreManager.getInstance();
        this.size += size;
        DataStore dataStore = manager.fetchStore(size);
        int offset = dataStore.write(buffer);
        Chunk chunk = new Chunk(dataStore, offset, size);
        chunkList.add(chunk);
    }

    public void finish() {
        this.finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * The len of data
     */
    public long size() {
        return this.size;
    }

    public InputStream getInputStream() {
        if (!finished) {
            throw new IllegalStateException("Http body not finished yet");
        }
        List<ByteBuffer> bufferList = chunkList.stream().map(Chunk::read).collect(toList());
        return new BufferListInputStream(bufferList);
    }

    /**
     * Get content as input stream, with content decompressed is needed
     */
    public InputStream getDecodedInputStream() {
        InputStream input = getInputStream();
        if (size() == 0) {
            return input;
        }

        try {
            if (contentEncoding == null || contentEncoding.isEmpty() || contentEncoding.equalsIgnoreCase("identity")) {
                // do nothing
            } else if ("gzip".equalsIgnoreCase(contentEncoding)) {
                input = new GZIPInputStream(input);
            } else if ("deflate".equalsIgnoreCase(contentEncoding)) {
                input = new InflaterInputStream(getInputStream(), new Inflater(true));
            } else if (contentEncoding.equalsIgnoreCase("compress")) {
                input = new ZCompressorInputStream(input);
            } else if ("br".equalsIgnoreCase(contentEncoding)) {
                input = new BrotliInputStream(input);
            } else if ("lzma".equalsIgnoreCase(contentEncoding)) {
                input = new LZMACompressorInputStream(input);
            } else {
                logger.warn("unsupported content-encoding: {}", contentEncoding);
            }
        } catch (Throwable t) {
            logger.error("Decode stream failed, encoding: {}", contentEncoding, t);
            return input;
        }
        return input;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(finished);
        out.writeObject(type);
        out.writeUTF(charset.name());
        out.writeUTF(contentEncoding);

        if (finished) {
            out.writeLong(size);
            try (InputStream in = getInputStream()) {
                in.transferTo(out);
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        finished = in.readBoolean();
        type = (BodyType) in.readObject();
        charset = Charset.forName(in.readUTF());
        contentEncoding = in.readUTF();

        if (finished) {
            size = in.readLong();
            chunkList = new ArrayList<>();
            loadFromInput(in, size);
            //TODO: check size
        }
    }

    public void loadFromInput(InputStream in, long size) throws IOException {
        if (size < 0) {
            size = Integer.MAX_VALUE;
        }
        int chunkSize = (int) Math.min(4 * 1024, size);

        long offset = 0;
        byte[] buffer = new byte[chunkSize];
        int read;
        while (offset < size && (read = in.read(buffer, 0, (int) Math.min(size - offset, chunkSize))) != -1) {
            append(ByteBuffer.wrap(buffer, 0, read));
            offset += read;
        }
    }

    @Override
    public String toString() {
        return "Body{size=" + size() + "}";
    }

    public BodyType getType() {
        return type;
    }

    public Charset getCharset() {
        return charset;
    }


    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setType(BodyType type) {
        this.type = type;
    }

}
