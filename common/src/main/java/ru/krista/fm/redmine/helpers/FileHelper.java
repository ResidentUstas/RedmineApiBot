package ru.krista.fm.redmine.helpers;

import lombok.NoArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

@NoArgsConstructor
public class FileHelper {

    public static byte[] decompress(byte[] data) throws IOException, DataFormatException {
        var inflater = new Inflater(true);
        inflater.setInput(data);

        var outputStream = new ByteArrayOutputStream(data.length);
        var buffer = new byte[1024];
        while (!inflater.finished()) {
            var count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }
}
