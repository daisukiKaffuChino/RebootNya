package github.daisukikaffuchino.rebootnya;

import github.daisukikaffuchino.rebootnya.shizuku.ShellResult;

interface IShellService {
    ShellResult exec(String cmd);
}
