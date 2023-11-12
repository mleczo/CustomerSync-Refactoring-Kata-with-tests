
import sys, os
from test_rig import main
import coverage


def coverage_main():
    coverage_datafile = os.path.join(os.environ["TEXTTEST_HOME"], "test", ".coverage")
    src_dir = os.path.join(os.environ["TEXTTEST_HOME"], "src")
    print(f"coverage will be written to {coverage_datafile}", file=sys.stderr)

    cov = coverage.Coverage(data_file=coverage_datafile, source=[src_dir], auto_data=True)
    cov.start()
    main()
    cov.stop()
    cov.save()

    cov.html_report(directory=os.path.join(os.environ["TEXTTEST_HOME"], "test", "htmlcov"))

if __name__ == '__main__':
    coverage_main()