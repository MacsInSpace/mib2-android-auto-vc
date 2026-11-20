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
# Constants) is identical.
# The cockpit (navsd) classes, when present (turns jar), checked against the
# K3342 decompile: RGStatus, DistanceToNextManeuver, ManeuverDescriptor and
# MapPresentation are the same as P4521. Two differ:
#  - Android Auto's guiding flag is ASL property 1920216832 (P4521: 895953920),
#    which RGStatus and InfoStates listen to;
#  - InfoStates has no CarLife listener;
#  - ActiveRgType has no FSG setup acknowledgement (stage 53): the call is
#    made a no-op, as K3342's own class does nothing there.
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

N=$S/de/vw/mib/bap/mqbab2/navsd/functions
if [ -d "$N" ]; then
    perl -pi -e 's/\b895953920\b/1920216832/g' "$N/RGStatus.java" "$N/InfoStates.java"
    perl -0777 -pi -e '
      s/^import de\.vw\.mib\.bap\.mqbab2\.common\.api\.carlife\.[^\n]*\n//mg;
      s/^CarLifeServiceListener,\n//m;
      s/^\s*protected static \/\*final\*\/ int\[\] CAR_LIFE_LISTENER_IDS[^\n]*\n//m;
      s/^\s*this\.getCarLifeService\(\)\.(add|remove)CarLifeServiceListener\([^\n]*\n//mg;
      s/ \|\| this\.getCarLifeService\(\)\.isCarLifeRouteGuidanceActive\(\)//;
      s/\n(\s*\/\/ \@Override\n)?\s*public void updateCarLifeData\(CarLifeService carLifeService, int n\) \{\n\s*this\.process\(-1\);\n\s*\}\n/\n/m;
    ' "$N/InfoStates.java"
    perl -0777 -pi -e '
      s/(private void notifyFsgSetupIfWaitingForAck\(Boolean bl\) \{\n)\s*int\[\] nArray = new int\[\]\{53\};\n\s*this\.context\.updateStages\(this, nArray, bl\);\n/$1        \/\/ K3342 has no FSG setup acknowledgement (stage 53).\n/;
    ' "$N/ActiveRgType.java"
    if grep -n "895953920" "$N/RGStatus.java" "$N/InfoStates.java"; then echo "k3342.sh: guiding flag id left" >&2; exit 1; fi
    if grep -n "CarLife" "$N/InfoStates.java"; then echo "k3342.sh: CarLife left in InfoStates" >&2; exit 1; fi
    if grep -n "updateStages" "$N/ActiveRgType.java"; then echo "k3342.sh: stage 53 call left" >&2; exit 1; fi
    grep -q "1920216832" "$N/RGStatus.java"
    grep -q "1920216832" "$N/InfoStates.java"
fi

# Every edit must have landed.
if grep -n "xbox[GM]\|initExBoxNavServices\|exboxGuidance" "$AAT"; then echo "k3342.sh: ExBox lines left" >&2; exit 1; fi
if grep -n "Baidu CarLife\|HdLiveMode\|JapanMarketOnly\|1114057728" "$CSI"; then echo "k3342.sh: CurrentStationInfo edits missing" >&2; exit 1; fi
grep -q "879176704" "$CSI"
echo "k3342.sh: sources adapted"
