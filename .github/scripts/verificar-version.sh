#!/usr/bin/env bash
# Verifica que el tag que se va a publicar sea un número de versión correcto, antes
# de compilar. Las actualizaciones automáticas (jDeploy) fallan con dos errores:
#
#   1. Un número menor que el último (p. ej. v1.4.1 después de v1.5.0) puede quedar
#      marcado como la versión más reciente. Las versiones siempre van hacia arriba.
#   2. Volver a publicar un número que ya salió: quien ya lo instaló no lo vuelve a
#      descargar. Si una versión sale mal, se corrige con la siguiente.
#
# También exige el formato v1.2.3, que es el que entiende jDeploy.
#
# Uso: bash .github/scripts/verificar-version.sh v1.2.3
# Necesita git (consulta los tags de origin) y gh con acceso al repositorio (GH_TOKEN en
# GitHub Actions) para saber si la versión ya se publicó.
set -euo pipefail

TAG="${1:?Indique el tag a publicar, p. ej. v1.2.3}"

if [[ ! "$TAG" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  echo "::error::\"$TAG\" no es un número de versión válido. Use el formato v1.2.3 (sin ceros a la izquierda)."
  exit 1
fi
VERSION="${TAG#v}"

# La versión más alta entre los demás tags v1.2.3 del repositorio.
ANTERIOR="$(git ls-remote --tags --refs origin 'v*' \
  | sed 's#.*refs/tags/##' \
  | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' \
  | grep -vxF "$TAG" \
  | sed 's/^v//' \
  | sort -V \
  | tail -n 1 || true)"

if [[ -n "$ANTERIOR" ]]; then
  MAYOR="$(printf '%s\n%s\n' "$ANTERIOR" "$VERSION" | sort -V | tail -n 1)"
  if [[ "$MAYOR" != "$VERSION" || "$ANTERIOR" == "$VERSION" ]]; then
    IFS=. read -r MA MI PA <<< "$ANTERIOR"
    echo "::error::La versión $VERSION no es mayor que la última ($ANTERIOR). Las versiones siempre van hacia arriba: para una corrección use v$MA.$MI.$((PA + 1)); para algo nuevo, v$MA.$((MI + 1)).0."
    exit 1
  fi
fi

# ¿Ya existe un release con este tag? Publicado: no se reutiliza. En borrador: es un
# intento anterior que falló a medias, y se puede volver a publicar.
if ESTADO="$(gh release view "$TAG" --json isDraft --jq '.isDraft' 2>&1)"; then
  if [[ "$ESTADO" == "false" ]]; then
    echo "::error::La versión $VERSION ya se publicó. No se reutilizan versiones: publique la corrección con un número nuevo."
    exit 1
  fi
  echo "Hay un borrador de $TAG de un intento anterior: se vuelve a publicar."
elif [[ "$ESTADO" != *"not found"* ]]; then
  echo "::error::No se pudo consultar si la versión $VERSION ya se publicó: $ESTADO"
  exit 1
fi

echo "Versión $VERSION correcta (la anterior es ${ANTERIOR:-ninguna})."
