import os, re

BACKEND_SRC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'backend', 'src', 'main', 'java')
OPENAPI_YAML = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'openapi', 'jingxuan-v1.yaml')

def extract_endpoints():
    """Scan Java Controllers and extract v1 API endpoints."""
    endpoints = {}
    for root, dirs, files in os.walk(BACKEND_SRC):
        for f in files:
            if not f.endswith('Controller.java'):
                continue
            fp = os.path.join(root, f)
            with open(fp, 'r', encoding='utf-8') as fh:
                content = fh.read()
            
            if '@V1Api' not in content and '/api/v1' not in content:
                continue
            
            base = ''
            m = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?\"([^\"]+)\"\s*\)', content)
            if m: base = m[1]
            
            for method, pattern in [('GET', r'@GetMapping\s*\(\s*\"([^\"]+)\"\s*\)'),
                                     ('POST', r'@PostMapping\s*\(\s*\"([^\"]+)\"\s*\)'),
                                     ('PUT', r'@PutMapping\s*\(\s*\"([^\"]+)\"\s*\)'),
                                     ('DELETE', r'@DeleteMapping\s*\(\s*\"([^\"]+)\"\s*\)')]:
                for m in re.finditer(pattern, content):
                    ep = m.group(1)
                    full = (base + ep) if (base.endswith('/') or ep.startswith('/')) else (base + '/' + ep)
                    full = re.sub(r'/+', '/', full)
                    if base.startswith('/api/v1') and ep.startswith('/api/v1'):
                        full = base
                    if '/api/v1' not in full:
                        continue
                    
                    rel = os.path.relpath(fp, BACKEND_SRC).replace('\\', '/')
                    if full not in endpoints:
                        endpoints[full] = {'methods': [], 'file': rel}
                    if method not in endpoints[full]['methods']:
                        endpoints[full]['methods'].append(method)
    return endpoints

def main():
    endpoints = extract_endpoints()
    sorted_paths = sorted(endpoints.keys())
    
    # Read old YAML header
    header_lines = []
    if os.path.exists(OPENAPI_YAML):
        with open(OPENAPI_YAML, 'r', encoding='utf-8') as f:
            old = f.read()
        # Keep everything before the first paths: section
        idx = old.find('\npaths:')
        if idx > 0:
            header_lines = old[:idx].split('\n')
    
    if not header_lines:
        header_lines = [
            'openapi: 3.1.0',
            'info:',
            '  title: Jingxuan API',
            '  version: 1.0.0',
            'servers:',
            '  - url: /api/v1',
        ]
    
    lines = list(header_lines)
    lines.append('')
    lines.append('paths:')
    
    for path in sorted_paths:
        info = endpoints[path]
        lines.append('  ' + path + ':')
        for method in info['methods']:
            ml = method.lower()
            pid = path.replace('/api/v1/', '', 1).replace('/', '_').replace('-', '_').replace('{', '').replace('}', '')
            op_id = ml + '_' + pid
            fname = info['file']
            lines.append('    ' + ml + ':')
            lines.append('      operationId: ' + op_id)
            lines.append('      summary: ' + fname)
            
            params = re.findall(r'\{(\w+)\}', path)
            if params:
                lines.append('      parameters:')
                for p in params:
                    lines.append('        - name: ' + p)
                    lines.append('          in: path')
                    lines.append('          required: true')
                    lines.append('          schema:')
                    lines.append('            type: string')
            
            lines.append('      responses:')
            status = '201' if method == 'POST' else ('204' if method == 'DELETE' else '200')
            lines.append('        "' + status + '":')
            lines.append('          description: ' + {'201': 'Created', '204': 'No Content', '200': 'OK'}[status])
            lines.append('        "401":')
            lines.append('          description: Unauthorized')
            lines.append('        "403":')
            lines.append('          description: Forbidden')
            lines.append('      security:')
            lines.append('        - BearerAuth: []')
    
    lines.append('')
    lines.append('components:')
    lines.append('  securitySchemes:')
    lines.append('    BearerAuth:')
    lines.append('      type: http')
    lines.append('      scheme: bearer')
    lines.append('      bearerFormat: JWT')
    lines.append('  schemas: {}')
    
    with open(OPENAPI_YAML, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')
    
    print(f'Synced {len(endpoints)} endpoints to {OPENAPI_YAML}')
    return 0

if __name__ == '__main__':
    exit(main())
