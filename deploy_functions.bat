@echo off
setlocal enabledelayedexpansion
echo ===================================================
echo  Nail-It Supabase Edge Functions Deployer
echo ===================================================
echo.
echo Step 1: Checking Supabase login...
call supabase login
if !ERRORLEVEL! neq 0 (
    echo.
    echo [ERROR] Supabase login failed or was cancelled.
    echo Please make sure you are logged in to deploy functions.
    pause
    exit /b !ERRORLEVEL!
)

echo.
echo Step 2: Deploying Edge Functions to project unegfymwpzicriyjhukl...
echo.

set FUNCTIONS=create_session submit_source_link prepare_asset_upload confirm_asset_upload create_try_on render_try_on generate_execution_package

for %%f in (%FUNCTIONS%) do (
    echo [Deploying] %%f...
    call supabase functions deploy %%f --project-ref unegfymwpzicriyjhukl --no-verify-jwt
    if !ERRORLEVEL! neq 0 (
        echo [ERROR] Failed to deploy %%f
    ) else (
        echo [SUCCESS] %%f deployed successfully!
    )
    echo ---------------------------------------------------
)

echo.
echo All deployment tasks completed!
pause
