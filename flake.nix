{
  description = "Minimal flake for java projects with embedded gradle cache";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/336eda0d07dc5e2be1f923990ad9fdb6bc8e28e3";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem ( system:
    let
      pkgs = import nixpkgs { inherit system; };
    in {
      devShells = rec {
        minimal = pkgs.mkShell {
          name = "java";
          buildInputs = [
            pkgs.jdk21
          ];
          shellHook = ''
            export GRADLE_USER_HOME="$PWD/gradle-cache";
          '';
        };

        with-jdt = minimal.overrideAttrs (finalAttrs: previousAttrs: {
          buildInputs = previousAttrs.buildInputs ++ [ pkgs.jdt-language-server ];
        });

        default = minimal;
      };
    });
}
