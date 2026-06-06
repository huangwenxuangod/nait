import sys
import re

def optimize_compose(file_path):
    unwanted = {'auth', 'imgproxy', 'meta', 'studio', 'supavisor'}
    
    with open(file_path, 'r') as f:
        content = f.read()

    # Split into lines
    lines = content.splitlines()
    output = []
    skip = False
    
    for line in lines:
        stripped = line.strip()
        if not stripped:
            output.append(line)
            continue
            
        indent = len(line) - len(line.lstrip())
        
        # If we are currently skipping an unwanted service block
        if skip:
            if indent > 2 or stripped.startswith('#'):
                continue
            else:
                skip = False
                
        # Detect start of a service block (at exactly 2 spaces indentation)
        if indent == 2 and stripped.endswith(':'):
            service_name = stripped[:-1].strip()
            if service_name in unwanted:
                skip = True
                continue
                
        # Remove dependencies on unwanted services in "depends_on" blocks
        # e.g., "      auth:" or "      - auth"
        if stripped.startswith('- ') or stripped.endswith(':'):
            dep_name = stripped.lstrip('- ').rstrip(':').strip()
            if dep_name in unwanted:
                # If it's a list item like "- auth", skip it
                if stripped.startswith('- '):
                    continue
                # If it's a key like "auth:", we can't easily skip without context, but standard supabase compose uses list format:
                # depends_on:
                #   db:
                #     condition: service_healthy
                #   auth:
                #     condition: service_started
                # We can skip the block if it starts with the unwanted service name
                
        output.append(line)

    # Let's do a second pass to clean up any empty depends_on blocks or block-style depends_on
    final_output = []
    skip_dep_block = False
    
    for i, line in enumerate(output):
        stripped = line.strip()
        indent = len(line) - len(line.lstrip())
        
        if skip_dep_block:
            if indent > 4:
                continue
            else:
                skip_dep_block = False
                
        # Check if this line is an unwanted dependency block (e.g. "auth:" under "depends_on:")
        if indent == 4 and stripped.endswith(':'):
            dep_name = stripped[:-1].strip()
            if dep_name in unwanted:
                skip_dep_block = True
                continue
                
        final_output.append(line)

    # Write back the optimized file
    with open(file_path, 'w') as f:
        f.write('\n'.join(final_output) + '\n')

if __name__ == '__main__':
    if len(sys.argv) > 1:
        optimize_compose(sys.argv[1])
    else:
        optimize_compose('docker-compose.yml')
