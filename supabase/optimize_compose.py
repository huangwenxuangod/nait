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
                
        # Detect dependency item in "depends_on"
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

    # Pass 3: Clean up empty "depends_on:" blocks
    cleaned_output = []
    i = 0
    while i < len(final_output):
        line = final_output[i]
        stripped = line.strip()
        
        if stripped == 'depends_on:':
            indent = len(line) - len(line.lstrip())
            # Look ahead to see if there are any child elements
            has_children = False
            j = i + 1
            while j < len(final_output):
                next_line = final_output[j]
                next_stripped = next_line.strip()
                if not next_stripped:
                    j += 1
                    continue
                next_indent = len(next_line) - len(next_line.lstrip())
                if next_indent > indent:
                    has_children = True
                break
            
            if not has_children:
                # Skip the "depends_on:" line entirely
                i += 1
                continue
                
        cleaned_output.append(line)
        i += 1

    # Write back the optimized file
    with open(file_path, 'w') as f:
        f.write('\n'.join(cleaned_output) + '\n')

if __name__ == '__main__':
    if len(sys.argv) > 1:
        optimize_compose(sys.argv[1])
    else:
        optimize_compose('docker-compose.yml')
