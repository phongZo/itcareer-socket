package itcareer.utils;

import com.google.gson.Gson;
import itcareer.model.MediaInfo;
import itcareer.model.TranscodeConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.KeyGenerator;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j2
public class FFmpegUtils {
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private static byte[] genAesKey ()  {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static Path genKeyInfo(String folder) throws IOException {
        byte[] aesKey = genAesKey();
        String iv = Hex.encodeHexString(Objects.requireNonNull(genAesKey()));
        if(aesKey == null) return null;
        Path keyFile = Paths.get(folder, "key");
        Files.write(keyFile, aesKey, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        String stringBuilder = "key" + LINE_SEPARATOR +
                keyFile.toString() + LINE_SEPARATOR +
                iv;

        Path keyInfo = Paths.get(folder, "key_info");

        Files.write(keyInfo, stringBuilder.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return keyInfo;
    }

    private static void genIndex(String file, String indexPath, String bandWidth) throws IOException {
        String stringBuilder = "#EXTM3U" + LINE_SEPARATOR +
                "#EXT-X-STREAM-INF:BANDWIDTH=" + bandWidth + LINE_SEPARATOR +
                indexPath;
        Files.write(Paths.get(file), stringBuilder.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static long handleConvertToM3U8(String sourcePath, String destFolder, TranscodeConfig config) throws IOException, InterruptedException {
        Path path = Paths.get(sourcePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Source path not found: " + sourcePath);
        }

        Path workDir = Paths.get(destFolder);
        Files.createDirectories(workDir);

        Dimension resolution = getVideoResolution(sourcePath);
        boolean include1080p = resolution.width >= 1920 && resolution.height >= 1080;

        List<String> commands = new ArrayList<>();
        commands.add("ffmpeg");
        commands.add("-i"); commands.add(sourcePath);

        // Map video/audio streams
        int qualityCount = include1080p ? 4 : 3;
        for (int i = 0; i < qualityCount; i++) {
            commands.add("-map"); commands.add("0:v:0");
            commands.add("-map"); commands.add("0:a:0");
        }

        commands.add("-c:v"); commands.add("libx264");
        commands.add("-crf"); commands.add("22");
        commands.add("-c:a"); commands.add("aac");
        commands.add("-ar"); commands.add("44100");

        // 360p
        commands.add("-filter:v:0"); commands.add("scale=w=480:h=360");
        commands.add("-maxrate:v:0"); commands.add("600k");
        commands.add("-b:a:0"); commands.add("500k");

        // 480p
        commands.add("-filter:v:1"); commands.add("scale=w=640:h=480");
        commands.add("-maxrate:v:1"); commands.add("1500k");
        commands.add("-b:a:1"); commands.add("1000k");

        // 720p
        commands.add("-filter:v:2"); commands.add("scale=w=1280:h=720");
        commands.add("-maxrate:v:2"); commands.add("3000k");
        commands.add("-b:a:2"); commands.add("2000k");

        // 1080p (optional)
        if (include1080p) {
            commands.add("-filter:v:3"); commands.add("scale=w=1920:h=1080");
            commands.add("-maxrate:v:3"); commands.add("5000k");
            commands.add("-b:a:3"); commands.add("256k");
        }

        // Stream map
        String streamMap = "v:0,a:0,name:360p v:1,a:1,name:480p v:2,a:2,name:720p";
        if (include1080p) {
            streamMap += " v:3,a:3,name:1080p";
        }
        commands.add("-var_stream_map"); commands.add(streamMap);

        commands.add("-preset"); commands.add("fast");
        commands.add("-hls_list_size"); commands.add("0");
        commands.add("-threads"); commands.add("0");
        commands.add("-f"); commands.add("hls");
        commands.add("-hls_flags"); commands.add("independent_segments");
        commands.add("-hls_time"); commands.add(config.getTsSeconds());
        commands.add("-master_pl_name"); commands.add("livestream.m3u8");
        commands.add("-y"); commands.add("livestream-%v.m3u8");

        Process process = new ProcessBuilder()
                .command(commands)
                .directory(workDir.toFile())
                .start();

        startInputAndErrorThreads(process);

        if (process.waitFor() != 0) {
            throw new RuntimeException("Something went wrong during FFmpeg processing");
        }

        if (!screenShots(sourcePath, String.join(File.separator, destFolder, "poster.jpg"))) {
            throw new RuntimeException("Cannot take thumbnail");
        }

        long duration = takeDuration(sourcePath);
        Files.delete(path);
        return duration;
    }

    public static Dimension getVideoResolution(String videoPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("ffprobe", "-v", "error", "-select_streams", "v:0",
                "-show_entries", "stream=width,height", "-of", "csv=p=0", videoPath);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        process.waitFor();

        if (line != null) {
            String[] parts = line.split(",");
            return new Dimension(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        throw new RuntimeException("Cannot read video resolution");
    }

    public static MediaInfo getMediaInfo(String source) throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add("ffprobe");
        commands.add("-i")    ;commands.add(source);
        commands.add("-show_format");
        commands.add("-show_streams");
        commands.add("-print_format") ;commands.add("json");

        Process process = new ProcessBuilder(commands)
                .start();

        MediaInfo mediaInfo = null;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            mediaInfo = new Gson().fromJson(bufferedReader, MediaInfo.class);
        } catch (IOException e) {
            log.debug("Error: " + e.getMessage());
        }

        if (process.waitFor() != 0) {
            return null;
        }

        return mediaInfo;
    }

    public static boolean screenShots(String source, String file) throws IOException, InterruptedException {

        List<String> commands = new ArrayList<>();
        commands.add("ffmpeg");
        commands.add("-i")    ;commands.add(source);
        commands.add("-vf")    ;commands.add("thumbnail,scale=1280:720");
        commands.add("-frames:v")  ;commands.add("1");
        commands.add(file);

        Process process = new ProcessBuilder(commands)
                .start();

        startInputAndErrorThreads(process);

        return process.waitFor() == 0;
    }

    public static long takeDuration(String source) throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add("ffmpeg");
        commands.add("-i");
        commands.add(source);

        Process process = new ProcessBuilder(commands)
                .redirectErrorStream(true)
                .start();

        long result = 0;
        // Read the output of the command
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("Duration")) {
                // Extract and print the duration information
                result =  extractDurationInSeconds(line);
                log.debug("Video Duration: {}", result);
                break;
            }
        }
        process.waitFor();
        //startInputAndErrorThreads(process);
        return result;
    }

    public static void startInputAndErrorThreads(Process process) {
        new Thread(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.debug(line);
                }
            } catch (IOException ignored) {
            }
        }).start();

        new Thread(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.debug(line);
                }
            } catch (IOException ignored) {
            }
        }).start();
    }

    private static long extractDurationInSeconds(String line) {
        // Assuming the duration information is in the format "Duration: HH:MM:SS.ss,"
        String[] parts = line.split(",")[0].split(":");
        int hours = Integer.parseInt(parts[1].trim());
        int minutes = Integer.parseInt(parts[2].trim());
        double seconds = Double.parseDouble(parts[3].trim());

        // Convert to total seconds
        return (long) (hours * 3600L + minutes * 60L + seconds);
    }
}
