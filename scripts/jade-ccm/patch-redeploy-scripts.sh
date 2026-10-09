#!/bin/bash
# Patch all redeploy scripts to use SA_REGISTRY_PASSWORD if available

for script in redeploy-*-quarkus redeploy-ccm-accessdedup-processor; do
    if [ ! -f "$script" ]; then
        continue
    fi

    echo "Patching $script..."

    # Replace docker login line with conditional that checks for SA_REGISTRY_PASSWORD
    sed -i.bak '127s|.*docker login.*|    # Use SA_REGISTRY_PASSWORD if available (from CI), otherwise use oc token\
    if [ -n "$SA_REGISTRY_PASSWORD" ]; then\
        echo "$SA_REGISTRY_PASSWORD" \| docker login -u github-deployer --password-stdin $image_registry_url\
    else\
        echo $(oc whoami -t) \| docker login -u $(oc whoami) --password-stdin $image_registry_url\
    fi|' "$script"

    echo "✅ Patched $script"
done

echo "All scripts patched!"
