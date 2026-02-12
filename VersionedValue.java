public class VersionedValue {
    public String value;
    public int version;

    public VersionedValue(String value, int version) {
        this.value = value;
        this.version = version;
    }

    @Override
    public String toString() {
        return "VersionedValue{value='" + value + "', version=" + version + "}";
    }
}
