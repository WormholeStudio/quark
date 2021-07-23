package studio.wormhole.quark;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.ParseResult;
import studio.wormhole.quark.command.BaseCommand;

@Component
public class CommandRunner implements CommandLineRunner, ExitCodeGenerator {

  private final BaseCommand baseCommand;

  private final IFactory factory;

  private int exitCode;

  public CommandRunner(BaseCommand baseCommand, IFactory factory) {
    this.baseCommand = baseCommand;
    this.factory = factory;
  }

  @Override
  public void run(String... args) throws Exception {
    exitCode = new CommandLine(baseCommand, factory)
        .setExecutionExceptionHandler(new PrintExceptionMessageHandler())
        .execute(args);
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }

  class PrintExceptionMessageHandler implements IExecutionExceptionHandler {

    public int handleExecutionException(Exception ex,
        CommandLine cmd,
        ParseResult parseResult) {
      cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));

      return cmd.getExitCodeExceptionMapper() != null
          ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
          : cmd.getCommandSpec().exitCodeOnExecutionException();
    }
  }
}
