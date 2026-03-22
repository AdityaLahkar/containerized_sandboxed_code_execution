package com.example.sandboxCode.code.service;

import com.example.sandboxCode.code.exception.ServerException;
import com.example.sandboxCode.code.model.CodeRequest;
import com.example.sandboxCode.code.model.CodeResponse;
import com.example.sandboxCode.code.service.strategy.LanguageStrategy;
import com.example.sandboxCode.code.service.strategy.StrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
public class ExecutorService{
    private final StrategyFactory strategyFactory;

    @Autowired
    public ExecutorService(StrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public CodeResponse execute(CodeRequest codeRequest){
        String jobId = UUID.randomUUID().toString();
        Path dirPath = Paths.get("/tmp/", jobId);
        try {
            Files.createDirectories(dirPath);
        } catch (Exception e) {
            throw new ServerException("Failed to create directory");
        }

        LanguageStrategy strategy = strategyFactory.getStrategy(codeRequest.getLanguage());

        Path filePath = dirPath.resolve("prog" + strategy.getFileExtension());

        try{
            Files.writeString(filePath, codeRequest.getCode());
        }catch (Exception e){
            throw new ServerException("Failed to write code to file");
        }

        try {
            return runCodeInDocker(jobId, dirPath, strategy);
        } finally {
            cleanup(jobId, dirPath);
        }
    }

    private CodeResponse runCodeInDocker(String jobId, Path dirPath, LanguageStrategy strategy){

        List<String> command = new ArrayList<>(Arrays.asList(
                "docker", "run", "--rm",
                "--name", "sandbox_" + jobId,
                "--network", "none",
                "--log-driver=none",
                "--memory", "128m",
                "--storage-opt", "size=50m",
                "--cpus", "0.5",
                "--pids-limit", "16",
                "-v", dirPath.toAbsolutePath() + ":/app:ro", // Read-only mount!
                strategy.getDockerImage()
        ));

        command.addAll(Arrays.asList(strategy.getExecutionCommand()));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            Thread streamReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (Exception ignored) {}
            });
            streamReader.start();

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                streamReader.interrupt();
                return new CodeResponse(
                        "Execution timed out",
                        "",
                        "timeout"
                );
            }

            streamReader.join();

            int exitCode = process.exitValue();

            if(exitCode == 0){
                return new CodeResponse(
                        "",
                        output.toString(),
                        "success"
                );
            }else{
                return new CodeResponse(
                        output.toString(),
                        output.toString(),
                        "error"
                );
            }
        }catch(Exception e){
            throw new ServerException("Failed to execute code in Docker");
        }
    }

    private void cleanup(String jobId, Path dirPath) {
        try {
            Process process = new ProcessBuilder("docker", "rm", "-f", "sandbox_" + jobId).start();
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        try {
            Files.walk(dirPath)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {}
        
    }

}

