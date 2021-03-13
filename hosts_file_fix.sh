#!/usr/bin/env bash

set -e -u -o pipefail

function get_line_number() {
  local file="${1}"
  local regex="${2}"
  echo $(grep -n "${regex}" "${file}" | cut -f1 -d:)
}

function swap_lines() {
  local file="${1}"
  {
      printf '%dm%d\n' "${2}" "${3}"
      printf '%d-m%d-\n' "${3}" "${2}"
      printf '%s\n' w q
  } | ed -s "${file}"
}

function hack_travis_hosts_file() {
  local file="${1}"
  local loopback=$(get_line_number "${file}" "127\.0\.0\.1")
  local travis=$(get_line_number "${file}" "127\.0\.1\.1")

  if [[ -n "${loopback}" && -n "${travis}" && "${loopback}" -gt "${travis}" ]]; then
    swap_lines "${file}" "${travis}" "${loopback}"
  fi
}

hack_travis_hosts_file "${1}"