#!/bin/bash

echo "==================================================="
echo " Nail-It Supabase Edge Functions Deployer"
echo "==================================================="
echo ""
echo "Step 1: Checking Supabase login..."
supabase login
if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Supabase login failed or was cancelled."
    echo "Please make sure you are logged in to deploy functions."
    exit 1
fi

echo ""
echo "Step 2: Deploying Edge Functions to project unegfymwpzicriyjhukl..."
echo ""

FUNCTIONS=(
    "create_session"
    "submit_source_link"
    "prepare_asset_upload"
    "confirm_asset_upload"
    "create_try_on"
    "generate_execution_package"
)

for f in "${FUNCTIONS[@]}"; do
    echo "[Deploying] $f..."
    supabase functions deploy "$f" --project-ref unegfymwpzicriyjhukl --no-verify-jwt
    if [ $? -ne 0 ]; then
        echo "[ERROR] Failed to deploy $f"
    else
        echo "[SUCCESS] $f deployed successfully!"
    fi
    echo "---------------------------------------------------"
done

echo ""
echo "All deployment tasks completed!"
