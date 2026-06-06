import sys

def optimize_compose(file_path):
    unwanted = {'auth', 'imgproxy', 'meta', 'studio', 'supavisor'}
    
    with open(file_path, 'r') as f:
        content = f.read()

    lines = content.splitlines()
    output = []
    skip_block = False
    skip_indent = 0
    
    # Pass 1: Remove unwanted services
    for line in lines:
        stripped = line.strip()
        if not stripped:
            output.append(line)
            continue
            
        indent = len(line) - len(line.lstrip())
        
        if skip_block:
            if indent > skip_indent:
                continue
            else:
                skip_block = False
                
        # Detect start of an unwanted service block under "services:" (typically at 2 spaces)
        if indent == 2 and stripped.endswith(':'):
            service_name = stripped[:-1].strip()
            if service_name in unwanted:
                skip_block = True
                skip_indent = indent
                continue
                
        output.append(line)

    # Pass 2: Remove unwanted dependencies under "depends_on:"
    final_output = []
    skip_dep = False
    skip_dep_indent = 0
    
    for line in output:
        stripped = line.strip()
        if not stripped:
            final_output.append(line)
            continue
            
        indent = len(line) - len(line.lstrip())
        
        if skip_dep:
            if indent > skip_dep_indent:
                continue
            else:
                skip_dep = False
                
        # Detect dependency item in "depends_on" (can be at 4, 6, or 8 spaces depending on formatting)
        # e.g., "imgproxy:" or "- imgproxy"
        if stripped.endswith(':'):
            dep_name = stripped[:-1].strip()
            if dep_name in unwanted:
                skip_dep = True
                skip_dep_indent = indent
                continue
        elif stripped.startswith('- '):
            dep_name = stripped[2:].strip()
            if dep_name in unwanted:
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
