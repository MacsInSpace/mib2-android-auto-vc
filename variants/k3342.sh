#!/bin/sh
# Turn a copy of src/ into the K3342 (MHI2_ER_VWG11_K3342, MU1427) sources.
# K3342 is an older generation than the P4521 these classes were taken from
# (checked against a K3342 lsd.jxe, #62):
#  - no ExBox guidance (ASLExboxmAPI.getExboxGuidanceManager,
#    ExboxGuidanceListenerImpl, RequestHandler.initExBoxNavServices);
#  - no Baidu CarLife source in CurrentStationInfo;
#  - the station-info serializer fields are ibocLiveTransmissionActive and
#    vicsAvailable (P4521: ...HdLiveMode, ...JapanMarketOnly);
#  - CurrentStationInfo listens to ASL property 879176704 for smartphone
#    integration (P4521: 1114057728).
# Everything else the music jar replaces (CoverArt, CurrentStationHandle,
# Constants) is identical. Only the music jar: the navsd classes (turns, full)
# differ more on K3342 and are not ported.
set -eu
S=$1
AAT=$S/de/vw/mib/asl/internal/androidauto/target/AndroidAutoTarget.java
CSI=$S/de/vw/mib/bap/mqbab2/audiosd/functions/CurrentStationInfo.java

perl -0777 -pi -e '
  s/^import de\.vw\.mib\.asl\.api\.exboxm\.[^\n]*\n//mg;
  s/^import de\.vw\.mib\.asl\.internal\.androidauto\.api\.impl\.ExboxGuidanceListenerImpl;\n//m;
  s/^\s*private ExboxGuidanceListenerImpl exboxGuidanceListenerImpl;\n//m;
  s/^\s*ExboxGuidanceManager exboxGuidanceManager = [^\n]*\n//m;
  s/^\s*this\.exboxGuidanceListenerImpl = new [^\n]*\n//m;
  s/^\s*this\.requestHandler\.initExBoxNavServices\([^\n]*\n//m;
  s/^\s*if \(this\.exboxGuidanceListenerImpl != null\) \{\n\s*this\.exboxGuidanceListenerImpl\.(startup|shutdown)\(\);\n\s*\}\n//mg;
' "$AAT"

perl -0777 -pi -e '
  s/^\s*case 4: \{\n\s*string = "Baidu CarLife";\n\s*n = 72;\n\s*break;\n\s*\}\n//m;
  s/ibocLiveTransmissionActiveHdLiveMode/ibocLiveTransmissionActive/g;
  s/vicsAvailableJapanMarketOnly/vicsAvailable/g;
  s/SMARTPHONE_INTEGRATION_LISTENER_IDS = new int\[\] \{ 1114057728 \}/SMARTPHONE_INTEGRATION_LISTENER_IDS = new int[] { 879176704 }/;
' "$CSI"

# Every edit must have landed.
if grep -n "xbox[GM]\|initExBoxNavServices\|exboxGuidance" "$AAT"; then echo "k3342.sh: ExBox lines left" >&2; exit 1; fi
if grep -n "Baidu CarLife\|HdLiveMode\|JapanMarketOnly\|1114057728" "$CSI"; then echo "k3342.sh: CurrentStationInfo edits missing" >&2; exit 1; fi
grep -q "879176704" "$CSI"
echo "k3342.sh: sources adapted"
