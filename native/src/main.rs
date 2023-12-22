mod launcher;

use std::process::ExitCode;

fn main() -> ExitCode {
    let mut iter = std::env::args();

    iter.next().expect("Missing executable file name");

    let command = match iter.next() {
        Some(command) => command,
        None => {
            print_help_message();
            return ExitCode::SUCCESS;
        }
    };

    return match command.as_str() {
        "help" | "-help" | "--help" | "-?" => {
            print_help_message();
            ExitCode::SUCCESS
        }
        "version" | "-version" | "--version" => {
            panic!("TODO: version");
        }
        "run" => {
            launcher::run_japp(iter.collect())
        }
        _ => {
            eprintln!("Unsupported command: {command}");
            ExitCode::FAILURE
        }
    };
}

fn print_help_message() {
    println!("TODO: Help Message")
}