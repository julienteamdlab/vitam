import {Component, OnInit, Input} from '@angular/core';
import {ArchiveUnitHelper} from "../../archive-unit.helper";

@Component({
  selector: 'vitam-archive-object-group',
  templateUrl: './archive-object-group.component.html',
  styleUrls: ['./archive-object-group.component.css']
})
export class ArchiveObjectGroupComponent implements OnInit {
  @Input() objects;
  translations;
  translate: (x) => string;
  displayObject = {};

  constructor(private archiveUnitHelper: ArchiveUnitHelper) {
    this.translations = this.archiveUnitHelper.getObjectGroupTranslations();
    this.translate = (field: string) => {
      const value = this.translations[field];
      if (this.translations[field]) {
        return value;
      } else {
        return field;
      }
    }
  }

  ngOnInit() {
  }

  toogleDetails(id) {
    this.displayObject[id] = !this.displayObject[id];
  }

  downloadObject(object) {
    // TODO DEVME
  }

  isDownloadable(version) {
    // FIXME: userContract.DataObjectVersion.indexOf(version.split('_')[0]) < 0
    return version.indexOf('BinaryMaster') !== -1;
  }

}