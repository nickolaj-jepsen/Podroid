# What a NixOS guest must provide for Podroid's AVF disk-image mode: a login on
# the hvc0 console the app's terminal shows, the vsock agent (control channel,
# terminal resize, port forwards), and the "Ready!" marker that BootStageDetector
# waits for before attaching the terminal. The kernel cmdline is the image's own
# (u-boot boots it), so podroid.* parameters never reach the guest.
{self}: {
  config,
  lib,
  pkgs,
  ...
}: let
  cfg = config.podroid.guest;
in {
  options.podroid.guest = {
    enable = lib.mkEnableOption "the Podroid AVF guest contract";
    package = lib.mkOption {
      type = lib.types.package;
      default = self.packages.${pkgs.stdenv.hostPlatform.system}.vsock-agent;
      defaultText = lib.literalExpression "podroid.packages.\${system}.vsock-agent";
      description = "The podroid-vsock-agent package.";
    };
  };

  config = lib.mkIf cfg.enable {
    boot.kernelParams = ["console=hvc0"];
    boot.kernelModules = ["vmw_vsock_virtio_transport"];

    # Port forwards and SSH arrive over the control channel at runtime; the seed only opens it.
    # The port matches VsockControlChannel.CTL_PORT.
    environment.etc."podroid/forwards.conf".text = ''
      9100 ctl
    '';

    # The app's TerminalView speaks xterm, not serial-getty's default vt220.
    systemd.services."serial-getty@hvc0".environment.TERM = "xterm-256color";

    systemd.services.podroid-vsock-agent = {
      description = "Podroid vsock control and port-forward agent";
      wantedBy = ["multi-user.target"];
      after = ["systemd-modules-load.service"];
      restartTriggers = [config.environment.etc."podroid/forwards.conf".source];
      serviceConfig = {
        ExecStart = lib.getExe cfg.package;
        Restart = "always";
        RestartSec = 1;
        OOMScoreAdjust = -1000;
      };
    };

    systemd.services.podroid-ready = {
      description = "Tell Podroid the guest is up";
      wantedBy = ["multi-user.target"];
      after = ["podroid-vsock-agent.service" "sshd.service" "network-online.target"];
      wants = ["network-online.target"];
      serviceConfig = {
        Type = "oneshot";
        RemainAfterExit = true;
      };
      # The marker sequence BootStageDetector matches.
      script = ''
        printf 'Starting SSH...\nAlmost ready...\nReady!\n' > /dev/console
      '';
    };
  };
}
