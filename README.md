# EasyTelnetServer

A simple telnet server for Java.
Support few VT100 functions.

## Example

	EasyShellServer srv = new EasyShellServer();
	
	srv.addCommand("echo", new Command() {
		@Override
		public void execute(String name, String argument, EasyTerminal terminal) throws IOException {
			terminal.write(argument);
			terminal.write("\r\n");
		}
	});
	
	srv.start(23);
