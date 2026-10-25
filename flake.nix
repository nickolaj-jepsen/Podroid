{
  description = "Podroid guest contract for NixOS images booted by the AVF disk-image mode";

  inputs.nixpkgs.url = "github:nixos/nixpkgs/nixos-26.05";

  outputs = {
    self,
    nixpkgs,
  }: let
    systems = ["aarch64-linux" "x86_64-linux"];
    forAll = f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
  in {
    packages = forAll (pkgs: {
      vsock-agent = pkgs.callPackage ./nix/vsock-agent.nix {};
      default = self.packages.${pkgs.stdenv.hostPlatform.system}.vsock-agent;
    });

    nixosModules.guest = import ./nix/guest.nix {inherit self;};
    nixosModules.default = self.nixosModules.guest;

    # `nix develop`, then `./gradlew assembleDebug` (native assets come from a release APK).
    devShells.x86_64-linux.default = import ./nix/android-shell.nix {
      pkgs = import nixpkgs {
        system = "x86_64-linux";
        config = {
          allowUnfree = true;
          android_sdk.accept_license = true;
        };
      };
    };
  };
}
