/*
 * Verifies that the skip flag works — no download is attempted.
 */
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log not found"

String log = buildLog.text
assert log.contains("skipped (tls.skip=true)") : "Expected skip message in log"
