use std::process::ExitCode;

pub fn run_japp(args: Vec<String>) -> ExitCode {
    let mut options = Vec::new();
    let mut japp_file = None;

    let mut iter = args.iter();
    while let Some(arg) = iter.next() {
        if arg.starts_with('-') {
            options.push(arg);
        } else {
            japp_file = Some(arg);
            break;
        }
    }

    let japp_file = japp_file.expect("Missing japp file name");

    panic!("TODO: run");
}