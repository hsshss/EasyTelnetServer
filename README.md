# EasyTelnetServer

A simple telnet server written in Java. <br />
Support (roughly) VT100 control sequences.

## Example

	EasyShellServer srv = new EasyShellServer();
	
	srv.addCommand("echo", new Command() {
		@Override
		public void execute(String name, String argument, EasyTerminal terminal) throws IOException {
			terminal.write(argument);
			terminal.write("\r\n");
			terminal.flush();
		}
	});
	
	srv.start(23);

## Support Telnet Client

  * PuTTY
  * TeraTerm
  * GNU telnet
  * Windows telnet

## License

Public Domain
