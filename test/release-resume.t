#!/usr/bin/env perl
use strict;
use warnings;
use Test::More;
use Cwd qw(getcwd);
use File::Path qw(make_path);
use File::Spec;
use File::Temp qw(tempdir);
use IPC::Open3;
use JSON::PP qw(decode_json encode_json);
use Symbol qw(gensym);

my $root = getcwd;
make_path('.cache');
my $temp = File::Spec->rel2abs(
    tempdir('resume-test-XXXXXX', DIR => '.cache', CLEANUP => 1));
make_path("$temp/bin");
my $fixture = "$temp/fixture.json";
my $log = "$temp/commands.jsonl";
my $state = "$temp/state.json";
my $make = $ENV{MAKE} // 'make';

sub write_file {
    my ($path, $source) = @_;
    open my $output, '>', $path or die "$path: $!";
    print {$output} $source;
    close $output or die "$path: $!";
}

sub read_file {
    my ($path) = @_;
    open my $input, '<', $path or die "$path: $!";
    local $/;
    return <$input>;
}

write_file("$temp/bin/gh", <<'PERL');
#!/usr/bin/env perl
use strict;
use warnings;
use JSON::PP qw(decode_json encode_json);
use MIME::Base64 qw(encode_base64);
sub read_json {
    open my $input, '<', $_[0] or die $!;
    local $/;
    return decode_json(<$input>);
}
open my $log, '>>', $ENV{RESUME_LOG} or die $!;
print {$log} encode_json({tool => 'gh', args => \@ARGV}), "\n";
close $log;
my $fixture = read_json($ENV{RESUME_FIXTURE});
if ($ARGV[0] eq 'run' && $ARGV[1] eq 'view') {
    exists $fixture->{runs}{$ARGV[2]} or die "Unknown run $ARGV[2]";
    print encode_json($fixture->{runs}{$ARGV[2]});
} elsif ($ARGV[0] eq 'api') {
    $ARGV[1] =~ /ref=([0-9a-f]+)/ or die 'Missing source SHA';
    print encode_base64($fixture->{meta}{$1}, '');
} elsif ($ARGV[0] eq 'run' && $ARGV[1] eq 'download') {
    my ($directory);
    for my $i (0 .. $#ARGV - 1) {
        $directory = $ARGV[$i + 1] if $ARGV[$i] eq '--dir';
    }
    defined $directory or die 'Missing download directory';
    open my $output, '>', "$directory/release-provenance.json" or die $!;
    print {$output} encode_json($fixture->{provenance});
    close $output;
} elsif ($ARGV[0] eq 'workflow' && $ARGV[1] eq 'run') {
    my $state = read_json($ENV{RESUME_STATE});
    $state->{last} = ++$state->{dispatches} == 1 ? '200' : '201';
    open my $output, '>', $ENV{RESUME_STATE} or die $!;
    print {$output} encode_json($state);
    close $output;
} elsif ($ARGV[0] eq 'run' && $ARGV[1] eq 'list') {
    print read_json($ENV{RESUME_STATE})->{last}, "\n";
} elsif ($ARGV[0] eq 'run' && $ARGV[1] eq 'watch') {
    exit($ENV{RESUME_WATCH_FAIL} // 0);
} else {
    die "Unexpected GitHub command: @ARGV";
}
PERL
write_file("$temp/bin/git", <<'PERL');
#!/usr/bin/env perl
use strict;
use warnings;
use JSON::PP qw(encode_json);
open my $log, '>>', $ENV{RESUME_LOG} or die $!;
print {$log} encode_json({tool => 'git', args => \@ARGV}), "\n";
close $log;
if ($ARGV[0] eq 'branch') { print "feature\n" }
elsif ($ARGV[0] eq 'status') { print " M user-work\n" if $ENV{RESUME_DIRTY} }
elsif ($ARGV[0] ne 'push' && $ARGV[0] ne 'tag') { die "Unexpected git: @ARGV" }
PERL
write_file("$temp/bin/sleep", "#!/usr/bin/env perl\nexit 0;\n");
chmod 0755, "$temp/bin/$_" for qw(gh git sleep);

my @build_names = ('Build Linux x64', 'Build Linux AArch64',
    'Build macOS ARM64', 'Build macOS x64', 'Build Windows x64',
    'Build FreeBSD x64', 'Build Windows ARM64', 'Build WASI Preview 1',
    'Build Python wheels');
my @normal_checks = ('Test Linux', 'Test macOS', 'Test Windows',
    'Test macOS x64 under Rosetta', 'Test WASI Preview 1',
    'Check cross-built artifact formats');

sub job { return {name => $_[0], conclusion => $_[1] // 'success'} }

sub scenario {
    my $data = {
        runs => {
            100 => {status => 'completed', headSha => 'a' x 40, jobs => [
                (map { job($_) } @build_names),
                job('Test Linux'), job('Test macOS', 'failure'),
                job('Test Windows', 'cancelled'),
                job('Test macOS x64 under Rosetta'), job('Test WASI Preview 1'),
                job('Check cross-built artifact formats', 'failure'),
            ]},
            200 => {status => 'completed', headSha => 'b' x 40, jobs => [
                (map { job($_) } 'Build Python wheels (retry)',
                    'Test macOS (retry)', 'Test Windows (retry)',
                    'Check cross-built artifact formats (retry)'),
            ]},
        },
        meta => {('a' x 40) => "version: 0.3.7\n",
                 ('b' x 40) => "version: 0.3.7\n"},
        provenance => {artifact_run_id => '100', version => '0.3.7'},
    };
    write_file($fixture, encode_json($data));
    write_file($state, encode_json({dispatches => 0}));
    write_file($log, '');
    return $data;
}

sub capture {
    my ($env, @command) = @_;
    local %ENV = %ENV;
    delete @ENV{qw(MAKEFLAGS MAKEOVERRIDES MAKEFILES GNUMAKEFLAGS MFLAGS
        MAKELEVEL GITHUB_OUTPUT RESUME_WATCH_FAIL RESUME_DIRTY)};
    $ENV{PATH} = "$temp/bin:$ENV{PATH}";
    $ENV{YS_RELEASE_GH} = "$temp/bin/gh";
    $ENV{RESUME_FIXTURE} = $fixture;
    $ENV{RESUME_STATE} = $state;
    $ENV{RESUME_LOG} = $log;
    @ENV{keys %$env} = values %$env;
    my $error = gensym;
    my $pid = open3(my $input, my $output, $error, @command);
    close $input;
    local $/;
    my $result = (<$output> // '') . (<$error> // '');
    waitpid($pid, 0);
    return ($? >> 8, $result);
}

sub helper {
    return capture({}, $^X, "$root/util/release-resume", @_);
}

subtest 'plan preserves successful checks and requires failed checks' => sub {
    scenario();
    my ($status, $json) = helper('plan', '0.3.7', '100');
    is($status, 0, 'cancelled overall run can supply successful builds');
    my $plan = decode_json($json);
    is($plan->{platforms}, 'macos windows freebsd-x64',
        'only failed and cancelled checks are selected');
    is($plan->{wheels_run_id}, '100', 'successful wheels are reused');
    ($status) = helper('verify', '0.3.7', '100');
    isnt($status, 0, 'incomplete validation cannot publish');
    ($status) = helper('verify', '0.3.7', '100', '200');
    is($status, 0, 'original passes and successful retries satisfy gates');
    my $output = "$temp/github-output";
    ($status) = capture({GITHUB_OUTPUT => $output}, $^X,
        "$root/util/release-resume", 'verify', '0.3.7', '100', '200');
    is($status, 0, 'workflow verification succeeds');
    is(read_file($output), "wheels_run_id=200\n", 'wheel provenance is output');
};

subtest 'failed checks and incorrect runs never bypass gates' => sub {
    my $data = scenario();
    $data->{runs}{100}{jobs}[0]{conclusion} = 'failure';
    write_file($fixture, encode_json($data));
    my ($status) = helper('plan', '0.3.7', '100', '200');
    isnt($status, 0, 'failed builds block artifact reuse');
    $data = scenario();
    $data->{runs}{100}{status} = 'in_progress';
    write_file($fixture, encode_json($data));
    ($status) = helper('plan', '0.3.7', '100');
    isnt($status, 0, 'incomplete source runs are rejected');
    $data = scenario();
    $data->{meta}{'b' x 40} = "version: 0.3.6\n";
    write_file($fixture, encode_json($data));
    ($status) = helper('verify', '0.3.7', '100', '200');
    isnt($status, 0, 'checks for a different version are rejected');
    $data = scenario();
    $data->{provenance}{artifact_run_id} = '999';
    write_file($fixture, encode_json($data));
    ($status) = helper('verify', '0.3.7', '100', '200');
    isnt($status, 0, 'checks against different binary builds are rejected');
    $data = scenario();
    $data->{provenance}{version} = '0.3.6';
    write_file($fixture, encode_json($data));
    ($status) = helper('verify', '0.3.7', '100', '200');
    isnt($status, 0, 'artifact provenance must match the release version');
    $data = scenario();
    push @{$data->{runs}{200}{jobs}}, job('Test Linux (retry)', 'failure');
    write_file($fixture, encode_json($data));
    ($status) = helper('verify', '0.3.7', '100', '200');
    isnt($status, 0, 'a failed retry cannot hide behind an older pass');
    $data = scenario();
    $data->{runs}{200}{jobs}[1]{conclusion} = 'failure';
    write_file($fixture, encode_json($data));
    ($status) = helper('verify', '0.3.7', '100', '200');
    isnt($status, 0, 'a failed platform retry blocks publication');
    ($status) = helper('verify', '0.3.7', '100', '', 'true');
    isnt($status, 0, 'untested mode still requires cross-built validation');
    ($status) = helper('verify', '0.3.7', '100', '200', 'true');
    is($status, 0, 'only explicit untested mode can skip platform tests');
};

subtest 'a subsequent retry retains every original gap' => sub {
    my $data = scenario();
    $data->{runs}{200}{jobs}[2]{conclusion} = 'failure';
    write_file($fixture, encode_json($data));
    my ($status, $json) = helper('plan', '0.3.7', '100', '200');
    is($status, 0, 'partial retry results can be inspected');
    my $plan = decode_json($json);
    is($plan->{platforms}, 'windows', 'only Windows is still failing');
    is($plan->{retry_platforms}, 'macos windows freebsd-x64',
        'new validation run will cover all original gaps');
};

my $make_source = read_file('Makefile');
my ($resume_target) = $make_source =~
    /(^release-resume:.*?)(?=^test-release-resume:)/ms;
defined $resume_target or die 'Cannot find release-resume target';
write_file("$temp/Makefile", "SHELL := bash\nROOT := $root\n" .
    "GH := $temp/bin/gh\nPERL := $^X\n$resume_target");

sub dispatches {
    return [grep { $_->{tool} eq 'gh' && $_->{args}[0] eq 'workflow' }
        map { decode_json($_) } split /\n/, read_file($log)];
}

subtest 'one command validates and publishes without rebuilding' => sub {
    scenario();
    my ($status, $result) = capture({}, $make, '--no-print-directory',
        '-C', $temp, 'release-resume', 'v=0.3.7', 'r=100');
    is($status, 0, 'resume command completes') or diag($result);
    my $calls = dispatches();
    is(scalar @$calls, 2, 'validation and publication are dispatched');
    like(join(' ', @{$calls->[0]{args}}),
        qr/tests_only=macos windows freebsd-x64/, 'only missing checks run');
    like(join(' ', @{$calls->[0]{args}}), qr/python_wheels_run_id=100/,
        'validation reuses the existing wheels');
    like(join(' ', @{$calls->[1]{args}}), qr/release_tests_run_id=200/,
        'publication is gated on the completed validation run');
    unlike(join(' ', @{$calls->[1]{args}}), qr/tests_only=/,
        'continuation is a publication run, not another test-only run');
    for my $call (@$calls) {
        like(join(' ', @{$call->{args}}), qr/test_artifacts_run_id=100/,
            'both stages reuse the successful binary builds');
    }
};

subtest 'validation failure or dirty checkout cannot publish' => sub {
    scenario();
    my ($status) = capture({RESUME_WATCH_FAIL => 1}, $make,
        '--no-print-directory', '-C', $temp, 'release-resume',
        'v=0.3.7', 'r=100');
    isnt($status, 0, 'failed validation stops continuation');
    is(scalar @{dispatches()}, 1, 'publication is never dispatched');
    scenario();
    ($status) = capture({RESUME_DIRTY => 1}, $make, '--no-print-directory',
        '-C', $temp, 'release-resume', 'v=0.3.7', 'r=100');
    isnt($status, 0, 'dirty checkout stops continuation');
    is(scalar @{dispatches()}, 0, 'dirty checkout never dispatches');
};

subtest 'already validated artifacts proceed directly to publication' => sub {
    my $data = scenario();
    $data->{runs}{100}{jobs} = [map { job($_) } @build_names, @normal_checks];
    write_file($fixture, encode_json($data));
    my ($status, $result) = capture({}, $make, '--no-print-directory',
        '-C', $temp, 'release-resume', 'v=0.3.7', 'r=100');
    is($status, 0, 'completed checks need no rerun') or diag($result);
    is(scalar @{dispatches()}, 1, 'only publication is dispatched');
    like(join(' ', @{dispatches()->[0]{args}}), qr/release_tests_run_id=100/,
        'the original successful run supplies all checks');
};

subtest 'an explicit validation run can resume publication' => sub {
    scenario();
    my ($status, $result) = capture({}, $make, '--no-print-directory',
        '-C', $temp, 'release-resume', 'v=0.3.7', 'r=100', 'rt=200');
    is($status, 0, 'completed retry checks need no new validation run')
        or diag($result);
    is(scalar @{dispatches()}, 1, 'only publication is dispatched');
    like(join(' ', @{dispatches()->[0]{args}}), qr/release_tests_run_id=200/,
        'the supplied validation run is passed through');
};

subtest 'installed snapshot testing never rebuilds or bootstraps' => sub {
    my $path = "$temp/snapshot";
    make_path("$path/core", "$path/bin");
    write_file("$path/bin/ys", 'installed binary');
    write_file("$path/lein", 'installed Leiningen');
    my ($target) = read_file('ys/Makefile') =~
        /(^ifdef[ ]YS_RELEASE_USE_INSTALLED_YS\n
          SNAPSHOT-TEST-DEPS.*?)(?=^test-unit:)/msx;
    defined $target or die 'Cannot find snapshot target';
    write_file("$path/Makefile", "ROOT := $path\nCLI-BIN := bin/ys\n" .
        "CLI-DEPS := rebuild\nLEIN := lein\n$target\nrebuild:\n" .
        "\t\@echo rebuilt > rebuilt\n");
    write_file("$path/core/Makefile", "shell:\n" .
        "\ttest '\$(YS)' = '$path/bin/ys'\n" .
        "\ttest \"\$\$YS_SNAPSHOT_TEST_BIN\" = '$path/bin/ys'\n" .
        "\t\@echo tested > ../tested\n");
    my ($status, $result) = capture({}, $make, '--no-print-directory',
        '-C', $path, 'test-snapshots', 'YS_RELEASE_USE_INSTALLED_YS=1');
    is($status, 0, 'installed snapshot target completes') or diag($result);
    ok(-f "$path/tested", 'snapshot tests use the installed binary');
    ok(!-f "$path/rebuilt", 'binary build dependencies are not run');
    unlink "$path/bin/ys" or die $!;
    ($status) = capture({}, $make, '--no-print-directory', '-C', $path,
        'test-snapshots', 'YS_RELEASE_USE_INSTALLED_YS=1');
    isnt($status, 0, 'missing installed binary is an error');
    ok(!-f "$path/rebuilt", 'missing binary does not trigger a rebuild');
};

done_testing;
