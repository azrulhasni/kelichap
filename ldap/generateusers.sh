#!/bin/bash

# Number of entries to generate
num_entries=1000

# Output file
filename="people.ldif"

# Password for all users
password="abc123"

# Write the organizational unit entry to the file
cat <<EOL > "$filename"
dn: ou=people,o=sevenSeas
objectClass: organizationalUnit
objectClass: top
description: Contains entries which describe persons
ou: people

EOL

# Function to generate a person entry
generate_person_entry() {
  local i=$1
  local manager_dn=""

  if (( i % 100 == 0 )); then
    # Top-level manager, no manager attribute
    manager_dn=""
  elif (( i % 10 == 0 )); then
    # Mid-level manager, reports to the nearest preceding top-level manager
    manager_dn="manager: uid=user$(( (i / 100) * 100 )),ou=people,o=sevenSeas"
  else
    # Regular user, reports to the nearest preceding mid-level manager
    manager_dn="manager: uid=user$(( (i / 10) * 10 )),ou=people,o=sevenSeas"
  fi

  cat <<EOL >> "$filename"
dn: uid=user${i},ou=people,o=sevenSeas
objectClass: inetOrgPerson
objectClass: organizationalPerson
objectClass: person
objectClass: top
cn: User ${i}
sn: ${i}
givenName: User
mail: user${i}@sevenseas.com
uid: user${i}
${manager_dn}
userPassword: ${password}

EOL
}

# Generate the person entries
for i in $(seq 1 $num_entries); do
  generate_person_entry $i
done

echo "LDIF file '$filename' has been generated with $num_entries entries."
