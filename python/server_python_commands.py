# commands to be used in the Jupyter notebook, this file is not to be run on
# its own

from subprocess import Popen, PIPE, STDOUT
from IPython.core.magic import register_line_magic

import os
import signal
from subprocess import Popen, PIPE

@register_line_magic
def runrealcmd(command):
    process = Popen(command, stdout=PIPE, shell=True, stderr=STDOUT, bufsize=1, close_fds=True)
    for line in iter(process.stdout.readline, b''):
        print(line.rstrip().decode('utf-8'))
    process.stdout.close()
    process.wait()

def kill_server(port):
    process = Popen(["lsof", "-i", ":{0}".format(port)], stdout=PIPE, stderr=PIPE)
    stdout, stderr = process.communicate()
    for process in str(stdout.decode("utf-8")).split("\n")[1:]:       
        data = [x for x in process.split(" ") if x != '']
        if (len(data) <= 1):
            continue

        os.kill(int(data[1]), signal.SIGKILL)