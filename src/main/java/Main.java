import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        FFmpeg ffmpeg = new FFmpeg("C:\\PATH_Programs\\ffmpeg.exe"); // укажи путь до ffmpeg.exe
        FFprobe ffprobe = new FFprobe("C:\\PATH_Programs\\ffprobe.exe"); // также до ffproge.exe

        FFmpegProbeResult input = ffprobe.probe("C:\\Games\\misha.mov"); // это место исходного видео (его будем преобразовывать)

        double durationInSeconds = input.getStreams().stream() // вытаскиваем данные о видеопотоках чтобы извлечь длинну видео
                .filter(s -> s.codec_type == FFmpegStream.CodecType.VIDEO)
                .findFirst()
                .map(s -> s.duration)
                .orElse(0.0);

        FFmpegBuilder builder = new FFmpegBuilder() // объект в котором ведётся настройка получаемого видео
                .setInput(input)
                .overrideOutputFiles(true) // Перезаписываем ли файл если имеется с таким же названием
                .addOutput("output.mp4")
                .setFormat("mp4") // должно быть такое же как и в addOutput
                .setVideoCodec("h264_nvenc") // видео кодек nvidia, обрабатывает с помощью видеокарты, по умолчанию ставьте libx264

                .addExtraArgs("-cq", "51") // Уровень качества (чем ниже значение, тем выше качество)
                .addExtraArgs("-b:v", "750k") // Максимальный битрейт видео
                .addExtraArgs("-preset", "slow") // Настройка для улучшенного качества
                .addExtraArgs("-profile:v", "high") // Профиль кодека

                .setVideoFrameRate(24, 1)
                .setVideoResolution(640, 480)

                .setAudioCodec("aac")
                .setAudioChannels(1)
                .setAudioSampleRate(48_000)
                .setAudioBitRate(32768)
                .done(); // заканчиваем настройку

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder, new ProgressListener() { // логирование прогресса в progress
            @Override
            public void progress(Progress progress) {
                double percentage = progress.out_time_ns / 1_000_000_000.0 / durationInSeconds * 100;
                System.out.printf("Progress: %.2f%%%n", percentage);

                System.out.printf("Current Time: %.2fs, Speed: %.2fx%n",
                        progress.out_time_ns / 1_000_000_000.0,
                        progress.speed);
            }
        }).run(); // запуск
    }
}
