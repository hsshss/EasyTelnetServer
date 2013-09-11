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
			terminal.flush();
		}
	});
	
	srv.start(23);

## How to connect

### Windows

	> telnet localhost -t VT100

### Other

  * PuTTY
  * TeraTerm
  * GNU telnet