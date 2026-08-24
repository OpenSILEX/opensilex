#!/usr/bin/env python3
"""
Swagger 2 (io.swagger.annotations) to OpenAPI 3 (io.swagger.v3.oas.annotations) Migration Script
OpenSILEX Project
"""

import os
import re
import sys

def migrate_java_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content

    # 1. Update imports
    content = re.sub(r'import\s+io\.swagger\.annotations\.Api;', 'import io.swagger.v3.oas.annotations.tags.Tag;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.ApiOperation;', 'import io.swagger.v3.oas.annotations.Operation;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.ApiParam;', 'import io.swagger.v3.oas.annotations.Parameter;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.ApiResponse;', 'import io.swagger.v3.oas.annotations.responses.ApiResponse;\nimport io.swagger.v3.oas.annotations.media.Content;\nimport io.swagger.v3.oas.annotations.media.Schema;\nimport io.swagger.v3.oas.annotations.media.ArraySchema;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.ApiResponses;', 'import io.swagger.v3.oas.annotations.responses.ApiResponses;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.ApiModel;', 'import io.swagger.v3.oas.annotations.media.Schema;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.ApiModelProperty;', 'import io.swagger.v3.oas.annotations.media.Schema;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.SwaggerDefinition;', 'import io.swagger.v3.oas.annotations.OpenAPIDefinition;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.ApiImplicitParam;', 'import io.swagger.v3.oas.annotations.Parameter;\nimport io.swagger.v3.oas.annotations.enums.ParameterIn;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.ApiImplicitParams;', 'import io.swagger.v3.oas.annotations.Parameters;', content)
    content = re.sub(r'import\s+io\.swagger\.annotations\.Info;\n?', '', content)

    # Handle wildcard imports
    if 'import io.swagger.annotations.*;' in content:
        v3_imports = (
            "import io.swagger.v3.oas.annotations.tags.Tag;\n"
            "import io.swagger.v3.oas.annotations.Operation;\n"
            "import io.swagger.v3.oas.annotations.Parameter;\n"
            "import io.swagger.v3.oas.annotations.Parameters;\n"
            "import io.swagger.v3.oas.annotations.enums.ParameterIn;\n"
            "import io.swagger.v3.oas.annotations.responses.ApiResponse;\n"
            "import io.swagger.v3.oas.annotations.responses.ApiResponses;\n"
            "import io.swagger.v3.oas.annotations.media.Schema;\n"
            "import io.swagger.v3.oas.annotations.media.Content;\n"
            "import io.swagger.v3.oas.annotations.media.ArraySchema;"
        )
        content = content.replace('import io.swagger.annotations.*;', v3_imports)

    # Deduplicate import lines
    lines = content.splitlines()
    seen_imports = set()
    new_lines = []
    for line in lines:
        if line.startswith('import ') and line.endswith(';'):
            if line in seen_imports:
                continue
            seen_imports.add(line)
        new_lines.append(line)
    content = "\n".join(new_lines) + ("\n" if content.endswith("\n") else "")

    # 2. Convert @Api -> @Tag
    content = re.sub(r'@Api\(([^)]+)\)', r'@Tag(name = \1)', content)
    content = re.sub(r'@Api\b', r'@Tag', content)

    # 3. Convert @ApiOperation -> @Operation
    content = re.sub(r'@ApiOperation\b', r'@Operation', content)

    # 4. Convert @ApiParam -> @Parameter
    content = re.sub(r'@ApiParam\b', r'@Parameter', content)

    # 5. Convert @ApiModel & @ApiModelProperty -> @Schema
    content = re.sub(r'@ApiModelProperty\b', r'@Schema', content)
    content = re.sub(r'@ApiModel\b', r'@Schema', content)

    # 6. Convert @ApiImplicitParams -> @Parameters and @ApiImplicitParam -> @Parameter
    content = re.sub(r'@ApiImplicitParams\b', r'@Parameters', content)
    content = re.sub(r'@ApiImplicitParam\b', r'@Parameter', content)

    # 7. Convert @Schema attributes (value -> description, notes -> description, dataType -> type)
    def fix_schema_annotation(match):
        args = match.group(1)
        args = re.sub(r'\bvalue\s*=', 'description =', args)
        args = re.sub(r'\bnotes\s*=', 'description =', args)
        args = re.sub(r'\brequired\s*=\s*true', 'requiredMode = Schema.RequiredMode.REQUIRED', args)
        args = re.sub(r'\brequired\s*=\s*false', 'requiredMode = Schema.RequiredMode.NOT_REQUIRED', args)
        args = re.sub(r'\bdataType\s*=', 'type =', args)
        args = re.sub(r',?\s*reference\s*=\s*"[^"]*"', '', args)
        return f'@Schema({args})'

    content = re.sub(r'@Schema\((.*?)\)', fix_schema_annotation, content, flags=re.DOTALL)
    content = re.sub(r',?\s*reference\s*=\s*"[^"]*"', '', content)

    # 8. Convert @Operation attributes (value -> summary, notes -> description)
    def fix_operation_annotation(match):
        args = match.group(1).strip()
        if args.startswith('"') and args.endswith('"'):
            return f'@Operation(summary = {args})'
        args = re.sub(r'\bvalue\s*=', 'summary =', args)
        args = re.sub(r'\bnotes\s*=', 'description =', args)
        return f'@Operation({args})'

    content = re.sub(r'@Operation\((.*?)\)', fix_operation_annotation, content, flags=re.DOTALL)

    # 9. Convert @Parameter attributes (value -> description, paramType -> in, dataType/type -> schema)
    def fix_parameter_annotation(match):
        args = match.group(1).strip()
        if args.startswith('"') and args.endswith('"'):
            return f'@Parameter(description = {args})'
        
        args = re.sub(r'\bvalue\s*=', 'description =', args)
        args = re.sub(r'\bparamType\s*=\s*"header"', 'in = ParameterIn.HEADER', args)
        args = re.sub(r'\bparamType\s*=\s*"query"', 'in = ParameterIn.QUERY', args)
        args = re.sub(r'\bparamType\s*=\s*"path"', 'in = ParameterIn.PATH', args)
        if 'schema = @Schema' not in args:
            args = re.sub(r'\bdataType\s*=\s*("[^"]*")', r'schema = @Schema(type = \1)', args)
            args = re.sub(r'\btype\s*=\s*("[^"]*")', r'schema = @Schema(type = \1)', args)
        args = re.sub(r'\ballowableValues\s*=\s*("[^"]*")', r'schema = @Schema(allowableValues = {\1})', args)
        args = re.sub(r'\bdefaultValue\s*=\s*("[^"]*")', r'schema = @Schema(defaultValue = \1)', args)
        return f'@Parameter({args})'

    content = re.sub(r'@Parameter\((.*?)\)', fix_parameter_annotation, content, flags=re.DOTALL)

    # 10. Convert @ApiResponse attributes (code -> responseCode, message -> description, response -> content)
    def fix_api_response(match):
        args = match.group(1).strip()
        args = re.sub(r'\bcode\s*=\s*(\d+)', r'responseCode = "\1"', args)
        args = re.sub(r'\bcode\s*=\s*("[^"]*")', r'responseCode = \1', args)
        args = re.sub(r'\bmessage\s*=', 'description =', args)
        
        has_list = 'responseContainer = "List"' in args or "responseContainer = 'List'" in args
        args = re.sub(r',?\s*responseContainer\s*=\s*"[A-Za-z0-9_]*"', '', args)
        
        resp_match = re.search(r'\bresponse\s*=\s*([A-Za-z0-9_.]+\.class)', args)
        if resp_match:
            cls = resp_match.group(1)
            if has_list:
                content_clause = f'content = @Content(array = @ArraySchema(schema = @Schema(implementation = {cls})))'
            else:
                content_clause = f'content = @Content(schema = @Schema(implementation = {cls}))'
            args = re.sub(r'\bresponse\s*=\s*[A-Za-z0-9_.]+\.class', content_clause, args)
        
        return f'@ApiResponse({args})'

    content = re.sub(r'@ApiResponse\((.*?)\)', fix_api_response, content, flags=re.DOTALL)

    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main(root_dir):
    modified_count = 0
    for root, _, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                if migrate_java_file(filepath):
                    print(f"Migrated: {filepath}")
                    modified_count += 1
    print(f"\nTotal files updated: {modified_count}")

if __name__ == '__main__':
    project_root = sys.argv[1] if len(sys.argv) > 1 else '.'
    main(project_root)
