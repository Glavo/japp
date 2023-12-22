use std::process::exit;

fn main() {
    let mut iter = std::env::args();

    iter.next().expect("Missing executable file name");

    let command = match iter.next() {
        Some(command) => command,
        None => {
            print_help_message();
            return;
        }
    };

    match command.as_str() {
        "help" | "-help" | "--help" | "-?" => {
            print_help_message();
            return;
        }
        "version" | "-version" | "--version" => {
            panic!("TODO: version");
        }
        "run" => {
            panic!("TODO: run");
        }
        _ => {
            eprintln!("Unsupported command: {command}");
            exit(1)
        }
    }
}

fn print_help_message() {
    println!("TODO: Help Message")
}