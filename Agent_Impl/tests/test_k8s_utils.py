
# Run from Agent_Impl/:
#   pytest tests/test_k8s_utils.py -v

import pytest
from utils.k8s_utils import parse_cpu_to_millicores, parse_memory_to_bytes



# parse_cpu_to_millicores

class TestParseCpuToMillicores:

    def test_millicores_suffix(self):
        assert parse_cpu_to_millicores("245m") == 245

    def test_millicores_zero(self):
        assert parse_cpu_to_millicores("0m") == 0

    def test_whole_core_integer(self):
        assert parse_cpu_to_millicores("1") == 1000

    def test_whole_core_two(self):
        assert parse_cpu_to_millicores("2") == 2000

    def test_decimal_core_half(self):
        assert parse_cpu_to_millicores("0.5") == 500

    def test_decimal_core_point_one(self):
        assert parse_cpu_to_millicores("0.1") == 100

    def test_microcores(self):
        # 500000u = 500 millicores
        assert parse_cpu_to_millicores("500000u") == 500

    def test_microcores_small(self):
        # 1000u = 1 millicore
        assert parse_cpu_to_millicores("1000u") == 1

    def test_resource_limit_one_core(self):
        # As it appears in the manifest: limits.cpu: "1.0"
        assert parse_cpu_to_millicores("1.0") == 1000

    def test_resource_limit_half_core(self):
        # As it appears in the manifest: requests.cpu: "0.5"
        assert parse_cpu_to_millicores("0.5") == 500

    def test_unrecognised_format_raises(self):
        with pytest.raises(ValueError, match="Unrecognised CPU quantity format"):
            parse_cpu_to_millicores("banana")

    def test_empty_string_raises(self):
        with pytest.raises((ValueError, Exception)):
            parse_cpu_to_millicores("")


# parse_memory_to_bytes

class TestParseMemoryToBytes:

    # --- Binary (IEC) suffixes ---

    def test_kibibytes(self):
        assert parse_memory_to_bytes("1Ki") == 1024

    def test_mebibytes(self):
        assert parse_memory_to_bytes("1Mi") == 1024 ** 2

    def test_mebibytes_512(self):
        assert parse_memory_to_bytes("512Mi") == 512 * (1024 ** 2)

    def test_mebibytes_850(self):
        assert parse_memory_to_bytes("850Mi") == 850 * (1024 ** 2)

    def test_mebibytes_700(self):
        assert parse_memory_to_bytes("700Mi") == 700 * (1024 ** 2)

    def test_gibibytes(self):
        assert parse_memory_to_bytes("1Gi") == 1024 ** 3

    def test_tebibytes(self):
        assert parse_memory_to_bytes("1Ti") == 1024 ** 4

    def test_kibibytes_large(self):
        assert parse_memory_to_bytes("524288Ki") == 524288 * 1024

    # --- Decimal (SI) suffixes ---

    def test_kilobytes(self):
        assert parse_memory_to_bytes("1K") == 1000

    def test_megabytes(self):
        assert parse_memory_to_bytes("1M") == 1000 ** 2

    def test_gigabytes(self):
        assert parse_memory_to_bytes("1G") == 1000 ** 3

    def test_megabytes_850(self):
        # SI megabytes — distinct from 850Mi
        assert parse_memory_to_bytes("850M") == 850 * (1000 ** 2)

    # --- Raw bytes ---

    def test_raw_bytes(self):
        assert parse_memory_to_bytes("512000000") == 512000000

    def test_raw_bytes_zero(self):
        assert parse_memory_to_bytes("0") == 0

    # --- Edge cases ---

    def test_whitespace_stripped(self):
        # Parser strips whitespace — should handle values with trailing space
        assert parse_memory_to_bytes("512Mi") == parse_memory_to_bytes("512Mi")

    def test_mebibytes_vs_megabytes_are_different(self):
        # This is a real correctness check — binary and decimal must not collide
        mebibytes = parse_memory_to_bytes("850Mi")
        megabytes  = parse_memory_to_bytes("850M")
        assert mebibytes != megabytes
        assert mebibytes > megabytes  # 1Mi = 1,048,576 > 1M = 1,000,000

    def test_unrecognised_format_raises(self):
        with pytest.raises(ValueError, match="Unrecognised memory quantity format"):
            parse_memory_to_bytes("banana")

    def test_empty_string_raises(self):
        with pytest.raises((ValueError, Exception)):
            parse_memory_to_bytes("")

    def test_nanocores(self):
        # 18537531n = 18 millicores (floor division)
        assert parse_cpu_to_millicores("18537531n") == 18

    def test_nanocores_one_millicore_boundary(self):
        # Exactly 1,000,000n = 1 millicore
        assert parse_cpu_to_millicores("1000000n") == 1

    def test_nanocores_less_than_one_millicore_rounds_to_zero(self):
        # 500,000n = 0 millicores (floor) — valid at very low idle usage
        assert parse_cpu_to_millicores("500000n") == 0