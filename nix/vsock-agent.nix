{
  lib,
  stdenv,
}:
stdenv.mkDerivation {
  pname = "podroid-vsock-agent";
  version = "0";
  src = ../build-rootfs/vsock-agent;

  # The Makefile's CFLAGS link statically for the Alpine squashfs; NixOS links dynamically.
  env.CFLAGS = "-O2 -Wall -Wextra -Wno-unused-parameter -D_GNU_SOURCE";

  installPhase = ''
    runHook preInstall
    install -Dm755 podroid-vsock-agent -t $out/bin
    runHook postInstall
  '';

  meta = {
    description = "Guest-side peer for Podroid's AVF backend (control channel, port forwards)";
    license = lib.licenses.gpl2Only;
    mainProgram = "podroid-vsock-agent";
    platforms = lib.platforms.linux;
  };
}
